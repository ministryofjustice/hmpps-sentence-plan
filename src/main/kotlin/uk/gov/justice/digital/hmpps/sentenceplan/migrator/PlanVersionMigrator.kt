package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.AAPService
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.GroupCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Requestable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Resolvable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Timeline
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.UpdateCollectionItemPropertiesCommand
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
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.runner.getCommandCount
import java.time.Duration
import java.time.LocalDateTime
import kotlin.collections.orEmpty

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
      response.commands.zip(commands).forEach { (response, request) ->
        when (response.result) {
          is AddCollectionItemCommandResult -> when ((request as AddCollectionItemCommand).collectionUuid) {
            context.goalsCollectionUuid -> context.createdGoalUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionItemUuid

            context.planAgreementsCollectionUuid -> context.createdGoalUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionItemUuid
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
    val goalsToRemove = { previous: Set<GoalEntity>, current: Set<GoalEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      previous.map { it.createdDate.toString() }.minus(current.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val g = current.first { g -> g.createdDate.toString() == createdDate }


          val removeGoalCommand = AddCollectionItemCommand(
            collectionUuid = context.goalsCollectionUuid,
            answers = mapOf(
              "title" to SingleValue(g.title),
              "target_date" to SingleValue(g.targetDate.toString()),
              "area_of_need" to SingleValue(AreasOfNeedMapper.map(g.areaOfNeed)),
              "related_areas_of_need" to MultiValue(
                g.relatedAreasOfNeed?.map { relatedAreaOfNeed -> AreasOfNeedMapper.map(relatedAreaOfNeed) }.orEmpty(),
              ),
            ),
            properties = mapOf(
              "status" to SingleValue(GoalStatusMapper.map(g.status)),
              "status_date" to SingleValue(g.statusDate.toString()),
            ),
            index = null,
            user = UserDetails.from(g.createdBy),
            assessmentUuid = context.assessmentUuid,
            createdOnTimestamp = g.createdDate,
          )

          commands.add(removeGoalCommand)
        }
      commands
    }

    val goalsToAdd = { previous: Set<GoalEntity>, current: Set<GoalEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.minus(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val g = current.first { g -> g.createdDate.toString() == createdDate }


          val addGoalCommand = AddCollectionItemCommand(
            collectionUuid = context.goalsCollectionUuid,
            answers = mapOf(
              "title" to SingleValue(g.title),
              "target_date" to SingleValue(g.targetDate.toString()),
              "area_of_need" to SingleValue(AreasOfNeedMapper.map(g.areaOfNeed)),
              "related_areas_of_need" to MultiValue(
                g.relatedAreasOfNeed?.map { relatedAreaOfNeed -> AreasOfNeedMapper.map(relatedAreaOfNeed) }.orEmpty(),
              ),
            ),
            properties = mapOf(
              "status" to SingleValue(GoalStatusMapper.map(g.status)),
              "status_date" to SingleValue(g.statusDate.toString()),
            ),
            index = null,
            user = UserDetails.from(g.createdBy),
            assessmentUuid = context.assessmentUuid,
            createdOnTimestamp = g.createdDate,
          )

          commands.add(addGoalCommand)

          val createStepsCommand = CreateCollectionCommand(
            user = UserDetails.from(g.createdBy),
            name = "STEPS",
            parentCollectionItem = addGoalCommand,
            assessmentUuid = context.assessmentUuid,
            createdOnTimestamp = g.createdDate,
          )

          commands.add(createStepsCommand)

          val createNotesCommand = CreateCollectionCommand(
            user = UserDetails.from(g.createdBy),
            name = "NOTES",
            parentCollectionItem = addGoalCommand,
            assessmentUuid = context.assessmentUuid,
            createdOnTimestamp = g.createdDate,
          )

          commands.add(createNotesCommand)
        }
      commands
    }

    val goalsToUpdate = { previous: Set<GoalEntity>, current: Set<GoalEntity> ->
          val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.intersect(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { g -> g.createdDate.toString() == createdDate.toString() }
          val p = previous.first { g -> g.createdDate.toString() == createdDate.toString() }


          if (c.goalOrder != p.goalOrder) {
            commands.add(
              ReorderCollectionItemCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                collectionItemUuid = context.createdGoalUuids[c.createdDate.toString()],
                index = c.goalOrder - 1,
              ),
            )
          }

          val answersUpdated = buildMap {
            if (c.title != p.title) put("title", SingleValue(c.title))
            if (c.targetDate != null && c.targetDate.toString() != p.targetDate.toString()) put(
              "target_date",
              SingleValue(c.targetDate.toString()),
            )
            if (c.areaOfNeed.name != p.areaOfNeed.name) put(
              "area_of_need",
              SingleValue(AreasOfNeedMapper.map(c.areaOfNeed)),
            )
            if (c.relatedAreasOfNeed != p.relatedAreasOfNeed) put(
              "related_areas_of_need",
              MultiValue(
                c.relatedAreasOfNeed?.map { relatedAreaOfNeed -> AreasOfNeedMapper.map(relatedAreaOfNeed) }.orEmpty(),
              ),
            )
          }

          commands.add(UpdateCollectionItemAnswersCommand(
            user = UserDetails.from(c.createdBy),
            assessmentUuid = context.assessmentUuid,
            added = answersUpdated,
            removed = emptyList(),
            collectionItemUuid = context.createdGoalUuids[c.createdDate.toString()],
          ))

          val propertiesUpdated = buildMap {
            if (c.status != p.status) put("status", SingleValue(GoalStatusMapper.map(c.status)))
            if (c.statusDate != null && c.statusDate.toString() != p.targetDate.toString()) put(
              "status_date",
              SingleValue(c.statusDate.toString()),
            )
          }

          commands.add(UpdateCollectionItemPropertiesCommand(
            user = UserDetails.from(c.createdBy),
            assessmentUuid = context.assessmentUuid,
            added = propertiesUpdated,
            removed = emptyList(),
            collectionItemUuid = context.createdGoalUuids[c.createdDate.toString()],
          ))
        }
    }
//
//    val goalsRemoved = current.goals.filter { goal ->
//      goalsToRemove(
//        context.previousGoals.toSet(),
//        current.goals,
//      ).contains(goal.createdDate)
//    }.map { goal ->
//      RemoveCollectionItemCommand(
//        collectionItemUuid = goal.uuid.toString(),
//        user = UserDetails.from(current.updatedBy),
//        assessmentUuid = context.assessmentUuid,
//      )
//    }.also { context.previousGoals = current.goals }
//
//    val goalsAdded = current.goals
//      .filter { goal ->
//        goalsToAdd(
//          context.previousGoals.toSet(),
//          current.goals,
//        ).contains(goal.createdDate)
//      }
//      .fold(mutableListOf<Requestable>()) { acc, goal ->
//        val addGoalCommand = AddCollectionItemCommand(
//          collectionUuid = context.goalsCollectionUuid,
//          answers = mapOf(
//            "title" to SingleValue(goal.title),
//            "target_date" to SingleValue(goal.targetDate.toString()),
//            "area_of_need" to SingleValue(AreasOfNeedMapper.map(goal.areaOfNeed)),
//            "related_areas_of_need" to MultiValue(
//              goal.relatedAreasOfNeed?.map { relatedAreaOfNeed -> AreasOfNeedMapper.map(relatedAreaOfNeed) }.orEmpty(),
//            ),
//          ),
//          properties = mapOf(
//            "status" to SingleValue(GoalStatusMapper.map(goal.status)),
//            "status_date" to SingleValue(goal.statusDate.toString()),
//          ),
//          index = null,
//          user = UserDetails.from(goal.createdBy),
//          assessmentUuid = context.assessmentUuid,
//        )
//
//        acc.add(addGoalCommand)
//
//        val createStepsCommand = CreateCollectionCommand(
//          user = UserDetails.from(goal.createdBy),
//          name = "STEPS",
//          parentCollectionItem = addGoalCommand,
//          assessmentUuid = context.assessmentUuid,
//        )
//
//        acc.add(createStepsCommand)
//
//        val createNotesCommand = CreateCollectionCommand(
//          user = UserDetails.from(goal.createdBy),
//          name = "NOTES",
//          parentCollectionItem = addGoalCommand,
//          assessmentUuid = context.assessmentUuid,
//        )
//
//        acc.add(createNotesCommand)
//
//        goal.steps.forEach { step ->
//          acc.add(
//            AddCollectionItemCommand(
//              user = UserDetails.from(step.createdBy),
//              collection = createStepsCommand,
//              answers = mapOf(
//                "actor" to SingleValue(ActorsMapper.map(step.actor)),
//                "status" to SingleValue(StepStatusMapper.map(step.status)),
//                "description" to SingleValue(step.description),
//              ),
//              properties = mapOf(
//                "status_date" to SingleValue(step.createdDate.toString()),
//              ),
//              index = null,
//              assessmentUuid = context.assessmentUuid,
//            ),
//          )
//        }
//
//        goal.notes.forEach { note ->
//          acc.add(
//            AddCollectionItemCommand(
//              user = UserDetails.from(note.createdBy),
//              collection = createNotesCommand,
//              answers = mapOf(
//                "note" to SingleValue(note.note),
//                "created_by" to SingleValue(note.createdBy?.username ?: "Unknown"),
//              ),
//              properties = mapOf(
//                "created_at" to SingleValue(note.createdDate.toString()),
//                "type" to SingleValue(GoalNoteTypeMapper.map(note.type)),
//              ),
//              index = null,
//              assessmentUuid = context.assessmentUuid,
//            ),
//          )
//        }
//
//        acc
//      }

    return listOf(
      *goalsToRemove(context.previousGoals, current.goals).toTypedArray(),
      *goalsToAdd(context.previousGoals, current.goals).toTypedArray(),
//      *goalsToUpdate(context.previousGoals, current.goals).toTypedArray(),
    )
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
        createdOnTimestamp = planAgreementNote.createdDate,
      )
    }

    return additions + deletions
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
