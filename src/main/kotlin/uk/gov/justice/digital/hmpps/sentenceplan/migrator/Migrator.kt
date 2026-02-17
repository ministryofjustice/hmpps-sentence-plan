package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Command
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RequestableCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Timeline
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.MultiValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.SingleValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value
import java.util.*

data class CommandsRequest(
  val commands: List<RequestableCommand>,
)

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

data class CommandsResponse(
  val commands: List<CommandResponse>,
) {
  inline fun <reified T : CommandResult> extractSingle(): T = commands.map { it.result }
    .filterIsInstance<T>()
    .singleOrNull()
    ?: error("Expected exactly one ${T::class.simpleName}")
}

data class PlanMigrationContext(
  val assessmentUuid: UUID,
  val goalsCollectionUuid: UUID,
  val goals: MutableMap<UUID, UUID> = mutableMapOf(),
  val planAgreementsCollectionUuid: UUID,
  val planAgreements: MutableMap<Long, UUID> = mutableMapOf(),
)

data class VersionDiff<T>(
  val added: List<T>,
  val removed: List<T>,
  val updated: List<Pair<T, T>>,
)

fun <T, K> diffByKey(
  previous: Set<T>,
  current: Set<T>,
  key: (T) -> K,
): VersionDiff<T> {
  val previousByKey = previous.associateBy(key)
  val currentByKey = current.associateBy(key)

  val added = current.filter { key(it) !in previousByKey }
  val removed = previous.filter { key(it) !in currentByKey }
  val updated = current.mapNotNull { curr ->
    previousByKey[key(curr)]?.let { prev -> prev to curr }
  }

  return VersionDiff(added, removed, updated)
}

@Component
@Profile("migration")
class Migrator(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
) : CommandLineRunner {
  @Qualifier("assessmentPlatformClient")
  private lateinit var assessmentPlatformClient: WebClient

  fun migrate(plan: PlanEntity) {
    val context = createContext(plan)

    val versions = plan.id
      ?.let(planVersionRepository::findAllByPlanId)
      .orEmpty()

    var previous: PlanVersionEntity? = null

    versions.forEach { current ->
      val goalCommands = migrateGoals(current, previous, context)
      val agreementNotesCommands = migratePlanAgreementNotes(current, previous, context)

      val commands = goalCommands + agreementNotesCommands

      if (goalCommands.isNotEmpty()) {
        dispatchCommands(commands)
      }

      previous = current
    }

    planRepository.save(plan.apply { migrated = true })
  }

  override fun run(vararg args: String) {
    var index = 0
    val pageSize = 25
    var hasNext = true
    while (hasNext) {
      val pageRequest = PageRequest.of(index++, pageSize)
      val batch = planRepository.findAllByMigratedFalse(pageRequest)

      if (batch.hasContent()) break

      batch.forEach { plan -> migrate(plan) }

      hasNext = batch.hasNext()
    }
  }

  fun createContext(plan: PlanEntity): PlanMigrationContext {
    val assessmentUuid =
      dispatchCommands(
        listOf(
          CreateAssessmentCommand(
            user = UserDetails.from(plan.createdBy),
            formVersion = "1",
            properties = emptyMap(),
            assessmentType = "SENTENCE_PLAN",
            timeline = null,
            identifiers = plan.crn?.let { mapOf(IdentifierType.CRN to it) },
          ),
        ),
      ).extractSingle<CreateAssessmentCommandResult>()
        .assessmentUuid

    val goalsCollectionUuid =
      dispatchCommands(
        listOf(
          CreateCollectionCommand(
            name = "GOALS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = assessmentUuid,
          ),
        ),
      ).extractSingle<CreateCollectionCommandResult>()
        .collectionUuid

    val planAgreementsCollectionUuid =
      dispatchCommands(
        listOf(
          CreateCollectionCommand(
            name = "PLAN_AGREEMENTS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = assessmentUuid,
          ),
        ),
      ).extractSingle<CreateCollectionCommandResult>()
        .collectionUuid

    return PlanMigrationContext(
      assessmentUuid = assessmentUuid,
      goalsCollectionUuid = goalsCollectionUuid,
      planAgreementsCollectionUuid = planAgreementsCollectionUuid,
    )
  }

  fun migrateGoals(
    current: PlanVersionEntity,
    previous: PlanVersionEntity?,
    context: PlanMigrationContext,
  ): List<RequestableCommand> {
    val diff: VersionDiff<GoalEntity> = diffByKey(
      previous = previous?.goals.orEmpty(),
      current = current.goals,
    ) { it.uuid }

    val removals = diff.removed.map {
      RemoveCollectionItemCommand(
        collectionItemUuid = context.goals[it.uuid]
          ?: throw RuntimeException("Unable to remove collection item ${it.uuid}"),
        user = UserDetails.from(current.updatedBy),
        assessmentUuid = context.assessmentUuid,
        timeline = Timeline(
          type = "GOAL_REMOVED",
          data = emptyMap(),
        ),
      )
    }

    diff.added.map { goal ->
      AddCollectionItemCommand(
        collectionUuid = context.goalsCollectionUuid,
        answers = mapOf(
          "title" to SingleValue(goal.title),
          "area_of_need" to SingleValue(goal.areaOfNeed.name),
          "related_areas_of_need" to MultiValue(
            goal.relatedAreasOfNeed?.map { it.name }.orEmpty(),
          ),
          "target_date" to SingleValue(goal.targetDate.toString()),
        ),
        properties = emptyMap(),
        index = goal.goalOrder,
        user = UserDetails.from(goal.createdBy),
        assessmentUuid = context.assessmentUuid,
        timeline = Timeline(
          type = "GOAL_ADDED",
          data = emptyMap(),
        ),
      )
        .let { command -> dispatchCommands(listOf(command)) }
        .extractSingle<AddCollectionItemCommandResult>()
        .also { result -> context.goals[goal.uuid] = result.collectionItemUuid }
    }

    val updates = diff.updated.flatMap { (prev, curr) ->
      val collectionUuid = context.goals[curr.uuid]
        ?: throw RuntimeException("Unable to update collection item ${curr.uuid}")

      buildList {
        if (prev.goalOrder != curr.goalOrder) {
          add(
            ReorderCollectionItemCommand(
              collectionItemUuid = collectionUuid,
              index = curr.goalOrder,
              user = UserDetails.from(curr.createdBy),
              assessmentUuid = context.assessmentUuid,
            ),
          )
        }

        val statusChanged = prev.status != curr.status

        val changedFields = buildMap<String, Value> {
          if (statusChanged) {
            put("status", SingleValue(curr.status.name))
            put("status_date", SingleValue(curr.statusDate.toString()))
          }
        }

        val timelineEvent = when (curr.status) {
          GoalStatus.ACHIEVED -> Timeline("GOAL_ACHIEVED", emptyMap())
          GoalStatus.REMOVED -> Timeline("GOAL_REMOVED", emptyMap())
          else -> null
        }

        if (changedFields.isNotEmpty()) {
          add(
            UpdateCollectionItemAnswersCommand(
              collectionItemUuid = collectionUuid,
              added = changedFields,
              removed = emptyList(),
              user = UserDetails.from(curr.createdBy),
              assessmentUuid = context.assessmentUuid,
              timeline = timelineEvent,
            ),
          )
        }
      }
    }

    return removals + updates
  }

  fun migratePlanAgreementNotes(
    current: PlanVersionEntity,
    previous: PlanVersionEntity?,
    context: PlanMigrationContext,
  ): List<RequestableCommand> {
    val diff: VersionDiff<PlanAgreementNoteEntity> = diffByKey(
      previous = previous?.agreementNotes.orEmpty(),
      current = current.agreementNotes,
    ) { it.id }

    val removals = diff.removed.map {
      RemoveCollectionItemCommand(
        collectionItemUuid = context.planAgreements[it.id]
          ?: throw RuntimeException("Unable to remove collection item ${it.id}"),
        user = UserDetails.from(current.updatedBy),
        assessmentUuid = context.assessmentUuid,
      )
    }

    diff.added.map { planAgreementNote ->
      AddCollectionItemCommand(
        collectionUuid = context.planAgreementsCollectionUuid,
        answers = buildMap {
          planAgreementNote.createdBy?.let { put("created_by", SingleValue(it.username)) }
          put("notes", SingleValue(planAgreementNote.agreementStatusNote))
        },
        properties = mapOf(
          "status" to SingleValue(planAgreementNote.agreementStatus.toString()),
          "status_date" to SingleValue(planAgreementNote.createdDate.toString()),
        ),
        index = null,
        user = UserDetails.from(planAgreementNote.createdBy),
        assessmentUuid = context.assessmentUuid,
      ).let { command -> dispatchCommands(listOf(command)) }
        .extractSingle<AddCollectionItemCommandResult>()
        .also { result -> context.planAgreements[planAgreementNote.id!!] = result.collectionItemUuid }
    }

    val updates = emptyList<RequestableCommand>()

    return removals + updates
  }

  fun dispatchCommands(commands: List<RequestableCommand>): CommandsResponse = assessmentPlatformClient
    .post()
    .uri("/command")
    .bodyValue(CommandsRequest(commands))
    .retrieve()
    .bodyToMono(CommandsResponse::class.java)
    .block()
    ?: throw RuntimeException("Empty response from Assessment Platform API")
}
