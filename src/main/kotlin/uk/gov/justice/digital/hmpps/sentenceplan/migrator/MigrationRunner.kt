package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import jdk.internal.net.http.common.Log.requests
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.MultiValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.SingleValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.ActorsMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.AgreementStatusMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.AreasOfNeedMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.GoalNoteTypeMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.GoalStatusMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.StepStatusMapper
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.String

class MigrationStats() {
  companion object {
    private lateinit var started: LocalDateTime
    var numberOfEvents = 0
    var numberOfVersions = 0
    fun start() {
      started = LocalDateTime.now()
    }

    fun getDuration(): Duration = Duration.between(started, LocalDateTime.now())
  }
}

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

fun List<Requestable>.getCommandCount(): Int = sumOf { command ->
  val added = when (command) {
    is GroupCommand -> 1 + command.commands.getCommandCount()
    else -> 1
  }
  added
}

@Component
class Migrator(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
  @param:Qualifier("assessmentPlatformClient")
  private val assessmentPlatformClient: WebClient,
  @param:Qualifier("coordinatorClient")
  private val coordinatorClient: WebClient,
) {
  @Transactional
  fun run(plan: PlanEntity) {
    val context = createContext(plan)

    var versionMappings: List<VersionMapping> = emptyList()

    try {
      val versions = plan.id
        ?.let(planVersionRepository::findAllByPlanId)
        .orEmpty()
        .filter { !it.softDeleted }
        .sortedBy { it.updatedDate }

      var lastUpdate: LocalDateTime? = null

      versionMappings = versions.map { current ->
        val versionClock = getClock(current.updatedDate)
        val versionTimestamp = listOfNotNull(lastUpdate, current.updatedDate)
          .maxBy { it.atZone(versionClock.zone).toInstant() }

        val updateAssessmentPropertiesCommand = UpdateAssessmentPropertiesCommand(
          user = UserDetails.from(current.createdBy),
          added = mapOf(
            "PLAN_TYPE" to SingleValue(current.planType.name),
          ),
          removed = emptyList(),
          assessmentUuid = context.assessmentUuid,
        )
        val goalCommands = migrateGoals(current, context)
        val agreementNotesCommands = migratePlanAgreementNotes(current, context)

        val commands: List<Requestable> = listOf(
          updateAssessmentPropertiesCommand,
          *goalCommands.toTypedArray(),
          *agreementNotesCommands.toTypedArray(),
        ).fold(emptyList()) { resolved, command ->
          resolved + (if (command is Resolvable) command.resolve(resolved) else command)
        }

        if (commands.isNotEmpty()) {
          log.info("Dispatching ${commands.getCommandCount()} commands for plan ${plan.id} version ${current.version}")
          val requestStarted = LocalDateTime.now()
          val response = dispatchCommand<GroupCommandResult>(
            versionTimestamp,
            GroupCommand(
              commands = commands,
              user = UserDetails.from(current.createdBy),
              assessmentUuid = context.assessmentUuid,
              timeline = Timeline("Daily version (migrated)", emptyMap()),
            ),
          )
          val requestDuration = Duration.between(requestStarted, LocalDateTime.now())
          log.info("${commands.getCommandCount()} executed in ${requestDuration.toMillis()} ms")
          response.commands.forEach { command ->
            when (command.request) {
              is AddCollectionItemCommand -> {
                with(command.result as AddCollectionItemCommandResult) {
                  when (command.request.collectionUuid) {
                    context.goalsCollectionUuid -> context.goals.add(collectionItemUuid)
                    context.planAgreementsCollectionUuid -> context.planAgreements.add(collectionItemUuid)
                  }
                }
              }

              else -> {}
            }
          }
          lastUpdate = LocalDateTime.now(versionClock)
        }

        MigrationStats.numberOfVersions += 1
        VersionMapping(
          version = current.version.toLong(),
          createdAt = current.updatedDate,
          event = current.status.name,
        )
      }

      migrateCoordinatorAssociations(
        MigrateAssociationRequest(
          mappings = versionMappings
            .mapNotNull { mapping ->
              when (mapping.event) {
                "LOCKED_INCOMPLETE" -> mapping.apply { event = "LOCKED" }
                "UNSIGNED" -> null
                else -> mapping
              }
            },
          entityUuidFrom = plan.uuid,
          entityUuidTo = UUID.fromString(context.assessmentUuid),
        ),
      )

      planRepository.save(plan.apply { migrated = true })
    } catch (e: Exception) {
      log.warn("Failed to migrate ${plan.id}: ${e.stackTraceToString()}")
      deleteAssessment(UUID.fromString(context.assessmentUuid))
    }
  }

  fun createContext(plan: PlanEntity): PlanMigrationContext {
    val response =
      dispatchCommands(
        plan.createdDate,
        commands = listOf(
          CreateAssessmentCommand(
            user = UserDetails.from(plan.createdBy),
            formVersion = "v1.0",
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

    val goalsAdded = current.goals
      .fold(mutableListOf<Requestable>()) { acc, goal ->
        val addGoalCommand = AddCollectionItemCommand(
          collectionUuid = context.goalsCollectionUuid,
          answers = mapOf(
            "title" to SingleValue(goal.title),
            "target_date" to SingleValue(goal.targetDate.toString()),
            "area_of_need" to SingleValue(AreasOfNeedMapper.map(goal.areaOfNeed)),
            "related_areas_of_need" to MultiValue(
              goal.relatedAreasOfNeed?.map { relatedAreaOfNeed -> AreasOfNeedMapper.map(relatedAreaOfNeed) }.orEmpty(),
            ),
          ),
          properties = mapOf(
            "status" to SingleValue(GoalStatusMapper.map(goal.status)),
            "status_date" to SingleValue(goal.statusDate.toString()),
          ),
          index = null,
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
                "actor" to SingleValue(ActorsMapper.map(step.actor)),
                "status" to SingleValue(StepStatusMapper.map(step.status)),
                "description" to SingleValue(step.description),
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
                "type" to SingleValue(GoalNoteTypeMapper.map(note.type)),
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
    }.also { context.planAgreements.clear() }

    val additions = current.agreementNotes.map { planAgreementNote ->
      AddCollectionItemCommand(
        collectionUuid = context.planAgreementsCollectionUuid,
        answers = mapOf(
          "notes" to SingleValue(planAgreementNote.agreementStatusNote),
          "created_by" to SingleValue(planAgreementNote.createdBy?.username ?: "Unknown"),
          "agreement_question" to SingleValue(planAgreementNote.agreementStatus.toString()),
        ),
        properties = mapOf(
          "status" to SingleValue(AgreementStatusMapper.map(planAgreementNote.agreementStatus)),
          "status_date" to SingleValue(planAgreementNote.createdDate.toString()),
        ),
        index = null,
        user = UserDetails.from(planAgreementNote.createdBy),
        assessmentUuid = context.assessmentUuid,
      )
    }

    return additions + deletions
  }

  private inline fun <reified T : CommandResult> dispatchCommand(timestamp: LocalDateTime, command: Requestable) =
    dispatchCommands(timestamp, listOf(command)).extractSingle<T>()

  fun dispatchCommands(timestamp: LocalDateTime, commands: List<Requestable>): CommandsResponse {
    val requestCommandCount = commands.getCommandCount()
    MigrationStats.numberOfEvents += requestCommandCount

    return assessmentPlatformClient
      .post()
      .uri { uriBuilder -> uriBuilder.path("/command").queryParam("backdateTo", timestamp.toString()).build() }
      .bodyValue(CommandsRequest(commands))
      .retrieve()
      .bodyToMono(CommandsResponse::class.java)
      .block()
      ?: throw RuntimeException("Empty response from Assessment Platform API")
  }

  fun deleteAssessment(assessmentUuid: UUID) = assessmentPlatformClient
    .delete()
    .uri("/assessment/$assessmentUuid")
    .retrieve()
    .toBodilessEntity()
    .block()

  data class VersionMapping(val version: Long, val createdAt: LocalDateTime, var event: String)
  data class MigrateAssociationRequest(
    val mappings: List<VersionMapping>,
    val entityUuidFrom: UUID,
    val entityUuidTo: UUID,
    val entityTypeFrom: String = "PLAN",
    val entityTypeTo: String = "AAP_PLAN",
  )

  fun migrateCoordinatorAssociations(request: MigrateAssociationRequest) {
    coordinatorClient
      .post()
      .uri("/oasys/migrate-associations")
      .bodyValue(request)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun getClock(date: LocalDateTime): Clock {
      val baseClock = Clock.systemDefaultZone()
      val backdateTo = date.atZone(baseClock.zone).toInstant()
      val offset = Duration.between(baseClock.instant(), backdateTo)
      return Clock.offset(baseClock, offset)
    }
  }
}

@Component
class MigrationRunner(
  private val planRepository: PlanRepository,
  private val migrator: Migrator,
) {
  fun run() {
    log.info("Starting migration")
    MigrationStats.start()

    val pageSize = 50
    var hasNext = true
    var totalPages: Int? = null
    var pageNumber = 0
    while (hasNext) {
      val pageRequest = PageRequest.of(0, pageSize)
      val page = planRepository.findAllByMigratedFalse(pageRequest)

      if (totalPages == null) {
        totalPages = page.totalPages
      }
      if (!page.hasContent()) break

      log.info("Migrating batch of ${page.content.size} items in page ${pageNumber++} of $totalPages")
      page.content.forEach { plan -> migrator.run(plan) }

      hasNext = page.hasNext()
    }
    log.info("Finished migration in ${MigrationStats.getDuration().toMinutes()} minutes")
    log.info("Migrated ${MigrationStats.numberOfVersions} versions and created ${MigrationStats.numberOfEvents} events")
  }


  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
