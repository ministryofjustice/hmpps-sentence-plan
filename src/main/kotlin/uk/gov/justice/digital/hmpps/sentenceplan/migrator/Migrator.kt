package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Command
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.GroupCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Requestable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Resolvable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.Timeline
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.MultiValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.SingleValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import kotlin.collections.plus

data class CommandsRequest(
  val commands: List<Requestable>,
)

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

data class CommandsResponse(
  val commands: List<CommandResponse>,
) {
  inline fun <reified T : CommandResult> extractNthInstance(index: Int): T = commands.map { it.result }
    .filterIsInstance<T>().getOrNull(index)
    ?: error("No response found at index $index for ${T::class.simpleName}")

  inline fun <reified T : CommandResult> extractSingle(): T = commands.map { it.result }
    .filterIsInstance<T>()
    .singleOrNull()
    ?: error("Expected exactly one ${T::class.simpleName}")
}

data class PlanMigrationContext(
  val assessmentUuid: String,
  val goalsCollectionUuid: String,
  val goals: MutableSet<String> = mutableSetOf(),
  val planAgreementsCollectionUuid: String,
  val planAgreements: MutableSet<String> = mutableSetOf(),
)

@Component
@Profile("migration")
class Migrator(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
) : CommandLineRunner {
  @Qualifier("assessmentPlatformClient")
  private lateinit var assessmentPlatformClient: WebClient

  override fun run(vararg args: String) {
    var index = 0
    val pageSize = 25
    var hasNext = true
    while (hasNext) {
      val pageRequest = PageRequest.of(index++, pageSize)
      val page = planRepository.findAllByMigratedFalse(pageRequest)

      if (page.hasContent()) break

      page.forEach { plan -> migrate(plan) }

      hasNext = page.hasNext()
    }
  }

  fun migrate(plan: PlanEntity) {
    val context = createContext(plan)

    val versions = plan.id
      ?.let(planVersionRepository::findAllByPlanId)
      .orEmpty()
      .filter { !it.softDeleted }
      .sortedBy { it.createdDate }

    versions.forEach { current ->
      val goalCommands = migrateGoals(current, context)
      val agreementNotesCommands = migratePlanAgreementNotes(current, context)

      val commands: List<Requestable> = listOf(*goalCommands.toTypedArray(), *agreementNotesCommands.toTypedArray())
        .fold(emptyList()) { resolved, command ->
          resolved + (if (command is Resolvable) command.resolve(resolved) else command) as Requestable
        }

      if (commands.isNotEmpty()) {
        dispatchCommand<CommandResult>(
          GroupCommand(
            commands = commands,
            user = UserDetails.from(current.createdBy),
            assessmentUuid = context.assessmentUuid,
            timeline = Timeline("Daily version (migrated)", emptyMap()),
          ),
        )
      }
    }

    planRepository.save(plan.apply { migrated = true })
  }

  fun createContext(plan: PlanEntity): PlanMigrationContext {
    val response =
      dispatchCommands(
        commands = listOf(
          CreateAssessmentCommand(
            user = UserDetails.from(plan.createdBy),
            formVersion = "1",
            properties = emptyMap(),
            assessmentType = "SENTENCE_PLAN",
            timeline = null,
            identifiers = plan.crn?.let { mapOf(IdentifierType.CRN to it) },
          ),
          CreateCollectionCommand(
            name = "GOALS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = "@0",
          ),
          CreateCollectionCommand(
            name = "PLAN_AGREEMENTS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = "@0",
          ),
        ),
      )

    return PlanMigrationContext(
      assessmentUuid = response.extractNthInstance<CreateAssessmentCommandResult>(0).assessmentUuid,
      goalsCollectionUuid = response.extractNthInstance<CreateCollectionCommandResult>(0).collectionUuid,
      planAgreementsCollectionUuid = response.extractNthInstance<CreateCollectionCommandResult>(1).collectionUuid,
    )
  }

  fun migrateGoals(
    current: PlanVersionEntity,
    context: PlanMigrationContext,
  ): List<Requestable> {
    val goalsRemoved = context.goals.map { goalUuid ->
      RemoveCollectionItemCommand(
        collectionItemUuid = goalUuid,
        user = UserDetails.from(current.updatedBy),
        assessmentUuid = context.assessmentUuid,
      )
    }.also { context.goals.clear() }

    val goalsAdded = current.goals.fold(mutableListOf<Requestable>()) { acc, goal ->
      val addGoalCommand = AddCollectionItemCommand(
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
      )

      acc.add(addGoalCommand)

      val createStepsCommand = CreateCollectionCommand(
        user = UserDetails.from(goal.createdBy),
        name = "STEPS",
        parentCollectionItem = addGoalCommand,
        assessmentUuid = context.assessmentUuid,
      )

      acc.add(createStepsCommand)

      val createNotesCommand = CreateCollectionCommand(
        user = UserDetails.from(goal.createdBy),
        name = "NOTES",
        parentCollectionItem = addGoalCommand,
        assessmentUuid = context.assessmentUuid,
      )

      acc.add(createNotesCommand)

      goal.steps.forEach { step ->
        acc.add(
          AddCollectionItemCommand(
            user = UserDetails.from(step.createdBy),
            collection = createStepsCommand,
            answers = mapOf(
              "actor" to SingleValue(step.actor),
              "description" to SingleValue(step.description),
              "status" to SingleValue(step.status.name),
            ),
            properties = mapOf(
              "status_date" to SingleValue(step.createdDate.toString()),
            ),
            index = null,
            assessmentUuid = context.assessmentUuid,
          ),
        )
      }

      goal.notes.forEach { note ->
        acc.add(
          AddCollectionItemCommand(
            user = UserDetails.from(note.createdBy),
            collection = createNotesCommand,
            answers = mapOf(
              "note" to SingleValue(note.note),
              "created_by" to SingleValue(note.createdBy?.username ?: "Unknown"),
            ),
            properties = mapOf(
              "created_at" to SingleValue(note.createdDate.toString()),
              "type" to SingleValue(note.type.name),
            ),
            index = null,
            assessmentUuid = context.assessmentUuid,
          ),
        )
      }

      acc
    }

    return goalsAdded + goalsRemoved
  }

  fun migratePlanAgreementNotes(
    current: PlanVersionEntity,
    context: PlanMigrationContext,
  ): List<Requestable> {
    val deletions = context.planAgreements.map { planAgreementUuid ->
      RemoveCollectionItemCommand(
        collectionItemUuid = planAgreementUuid,
        user = UserDetails.from(current.updatedBy),
        assessmentUuid = context.assessmentUuid,
      )
    }

    val additions = current.agreementNotes.map { planAgreementNote ->
      AddCollectionItemCommand(
        collectionUuid = context.planAgreementsCollectionUuid,
        answers = buildMap {
          planAgreementNote.createdBy?.let { put("created_by", SingleValue(it.username)) }
          put("notes", SingleValue(planAgreementNote.agreementStatusNote))
          put("agreement_question", SingleValue(planAgreementNote.agreementStatus.toString()))
        },
        properties = mapOf(
          "status" to SingleValue(planAgreementNote.agreementStatus.toString()),
          "status_date" to SingleValue(planAgreementNote.createdDate.toString()),
        ),
        index = null,
        user = UserDetails.from(planAgreementNote.createdBy),
        assessmentUuid = context.assessmentUuid,
      )
    }

    return additions + deletions
  }

  private inline fun <reified T : CommandResult> dispatchCommand(command: Requestable) = dispatchCommands(listOf(command)).extractSingle<T>()

  fun dispatchCommands(commands: List<Requestable>): CommandsResponse = assessmentPlatformClient
    .post()
    .uri("/command")
    .bodyValue(CommandsRequest(commands))
    .retrieve()
    .bodyToMono(CommandsResponse::class.java)
    .block()
    ?: throw RuntimeException("Empty response from Assessment Platform API")
}
