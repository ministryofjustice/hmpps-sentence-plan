package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.AAPService
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.GroupCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Requestable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Resolvable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Timeline
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.getCommandCount
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request.CommandResponse
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.MultiValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.SingleValue
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator.VersionMapping
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.ActorsMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.AgreementStatusMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.AreasOfNeedMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.GoalNoteTypeMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.GoalStatusMapper
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers.StepStatusMapper
import java.time.Duration
import java.time.LocalDateTime

@Component
class PlanVersionMigrator(
  private val aapService: AAPService,
) {
  fun migrate(context: Context, planVersion: PlanVersionEntity): VersionMapping {
    val updateAssessmentPropertiesCommand = UpdateAssessmentPropertiesCommand(
      user = UserDetails.from(planVersion.createdBy),
      added = mapOf(
        "PLAN_TYPE" to SingleValue(planVersion.planType.name),
      ),
      removed = emptyList(),
      assessmentUuid = context.assessmentUuid,
    )
    val goalCommands = migrateGoals(planVersion, context)
    val agreementNotesCommands = migratePlanAgreementNotes(planVersion, context)

    val commands: List<Requestable> = listOf(
      updateAssessmentPropertiesCommand,
      *goalCommands.toTypedArray(),
      *agreementNotesCommands.toTypedArray(),
    ).fold(emptyList()) { resolved, command ->
      resolved + (if (command is Resolvable) command.resolve(resolved) else command)
    }

    if (commands.isNotEmpty()) {
      log.info("Dispatching ${commands.getCommandCount()} commands for plan ${context.plan.id} version ${planVersion.version}")
      val requestStarted = LocalDateTime.now()
      val response = aapService.dispatchCommand<GroupCommandResult>(
        planVersion.updatedDate,
        GroupCommand(
          commands = commands,
          user = UserDetails.from(planVersion.createdBy),
          assessmentUuid = context.assessmentUuid,
          timeline = Timeline("Daily version (migrated)", emptyMap()),
        ),
      )
      val requestDuration = Duration.between(requestStarted, LocalDateTime.now())
      log.info("${commands.getCommandCount()} executed in ${requestDuration.toMillis()} ms")
      response.commands.forEach { command: CommandResponse ->
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
    }

    Stats.numberOfVersions += 1

    return VersionMapping(
      version = planVersion.version.toLong(),
      createdAt = planVersion.updatedDate,
      event = planVersion.status.name,
    )
  }

  fun migrateGoals(
    current: PlanVersionEntity,
    context: Context,
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
              goal.relatedAreasOfNeed?.map { relatedAreaOfNeed ->
                AreasOfNeedMapper.map(
                  relatedAreaOfNeed,
                )
              }.orEmpty(),
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
    context: Context,
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
