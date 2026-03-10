package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
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
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CreateCollectionCommandResult
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
    val updateAssessmentCommands = buildList {
      if (context.previousPlanVersion == null || context.previousPlanVersion != planVersion.planType.name) {
        add(
          UpdateAssessmentPropertiesCommand(
            user = UserDetails.from(planVersion.createdBy),
            added = mapOf(
              "PLAN_TYPE" to SingleValue(planVersion.planType.name),
            ),
            removed = emptyList(),
            assessmentUuid = context.assessmentUuid,
          ),
        )
      }
    }

    val goalCommands = migrateGoals(planVersion, context)
    val agreementNotesCommands = migratePlanAgreementNotes(planVersion, context)

    val commands: List<Requestable> = listOfNotNull(
      *updateAssessmentCommands.toTypedArray(),
      *goalCommands.toTypedArray(),
      *agreementNotesCommands.toTypedArray(),
    ).fold(emptyList()) { resolved, command ->
      resolved + (if (command is Resolvable) command.resolve(resolved) else command)
    }

    if (commands.isNotEmpty()) {
      log.info("Dispatching ${commands.getCommandCount()} commands for plan ${context.plan.id} version ${planVersion.version}")
      // TODO: Remove this debug log
//      log.info(ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(commands))
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

            context.planAgreementsCollectionUuid -> context.createdAgreementNotesUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionItemUuid

            in context.stepCollectionUuids.values -> context.createdStepsUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionItemUuid

            in context.notesCollectionUuids.values -> context.createdNotesUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionItemUuid
          }

          is CreateCollectionCommandResult -> when ((request as CreateCollectionCommand).name) {
            "STEPS" -> context.stepCollectionUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionUuid

            "NOTES" -> context.notesCollectionUuids[request.createdOnTimestamp.toString()] =
              response.result.collectionUuid
          }

          else -> {}
        }
      }
    } else {
      log.info("No commands to execute")
    }

    Stats.numberOfVersions += 1

    return VersionMapping(
      version = planVersion.version.toLong(),
      createdAt = planVersion.updatedDate,
      event = planVersion.status.name,
    )
  }

  fun migrateGoals(
    currentVersion: PlanVersionEntity,
    context: Context,
  ): List<Requestable> {
    val goalsToRemove = { previous: Set<GoalEntity>, current: Set<GoalEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      previous.map { it.createdDate.toString() }.minus(current.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val p = previous.first { g -> g.createdDate.toString() == createdDate }

          // TODO: Remove this debug log
          log.info("REMOVE GOAL")
          log.info(context.createdGoalUuids[p.createdDate.toString()])

          val removeGoalCommand = RemoveCollectionItemCommand(
            user = UserDetails.from(currentVersion.updatedBy),
            assessmentUuid = context.assessmentUuid,
            collectionItemUuid = context.createdGoalUuids[p.createdDate.toString()],
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

    val goalsToBeAdded = goalsToAdd(context.previousGoals, currentVersion.goals).toTypedArray()

    val goalsToUpdate = { previous: Set<GoalEntity>, current: Set<GoalEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.intersect(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { g -> g.createdDate.toString() == createdDate }
          val p = previous.first { g -> g.createdDate.toString() == createdDate }


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

          if (answersUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemAnswersCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = answersUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdGoalUuids[c.createdDate.toString()],
              ),
            )
          }

          val propertiesUpdated = buildMap {
            if (c.status != p.status) put("status", SingleValue(GoalStatusMapper.map(c.status)))
            if (c.statusDate != null && c.statusDate.toString() != p.targetDate.toString()) put(
              "status_date",
              SingleValue(c.statusDate.toString()),
            )
          }

          if (propertiesUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemPropertiesCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = propertiesUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdGoalUuids[c.createdDate.toString()],
              ),
            )
          }
        }
      commands
    }

    val stepsToRemove = { previous: Set<StepEntity>, current: Set<StepEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      previous.map { it.createdDate.toString() }.minus(current.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val p = previous.first { it.createdDate.toString() == createdDate }

          // TODO: Remove this log
          log.info("REMOVE STEP")
          log.info(ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(context.createdStepsUuids))
          log.info(
            ObjectMapper().writerWithDefaultPrettyPrinter()
              .writeValueAsString(
                previous.map { it.createdDate.toString() }
                  .minus(current.map { it.createdDate.toString() }.toSet()),
              ),
          )

          val removeStepCommand = RemoveCollectionItemCommand(
            user = UserDetails.from(currentVersion.updatedBy),
            assessmentUuid = context.assessmentUuid,
            collectionItemUuid = context.createdStepsUuids[p.createdDate.toString()],
          )

          commands.add(removeStepCommand)
        }
      commands
    }

    val stepsToAdd = { previous: Set<StepEntity>, current: Set<StepEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.minus(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { it.createdDate.toString() == createdDate }

          commands.add(
            AddCollectionItemCommand(
              collection = goalsToBeAdded.firstOrNull {
                it is CreateCollectionCommand
                  && it.createdOnTimestamp.toString() == c.goal?.createdDate.toString()
                  && it.name == "STEPS"
              } as? CreateCollectionCommand,
              collectionUuid = context.stepCollectionUuids[c.goal?.createdDate.toString()],
              answers = mapOf(
                "actor" to SingleValue(ActorsMapper.map(c.actor)),
                "status" to SingleValue(StepStatusMapper.map(c.status)),
                "description" to SingleValue(c.description),
              ),
              properties = mapOf(
                "status_date" to SingleValue(c.createdDate.toString()),
              ),
              index = null,
              user = UserDetails.from(c.createdBy),
              assessmentUuid = context.assessmentUuid,
              createdOnTimestamp = c.createdDate,
            ),
          )
        }
      commands
    }

    val stepsToUpdate = { previous: Set<StepEntity>, current: Set<StepEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.intersect(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { it.createdDate.toString() == createdDate }
          val p = previous.first { it.createdDate.toString() == createdDate }

          val answersUpdated = buildMap {
            if (c.actor != p.actor) put("actor", SingleValue(ActorsMapper.map(c.actor)))
            if (c.status.name != p.status.name) put(
              "status",
              SingleValue(StepStatusMapper.map(c.status)),
            )
            if (c.description != p.description) put(
              "description",
              SingleValue(c.description),
            )
          }

          if (answersUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemAnswersCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = answersUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdStepsUuids[c.createdDate.toString()],
              ),
            )
          }

          val propertiesUpdated = buildMap {
            if (c.createdDate.toString() != p.createdDate.toString()) {
              put("status_date", SingleValue(c.createdDate.toString()))
            }
          }

          if (propertiesUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemPropertiesCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = propertiesUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdStepsUuids[c.createdDate.toString()],
              ),
            )
          }
        }
      commands
    }

    val notesToRemove = { previous: Set<GoalNoteEntity>, current: Set<GoalNoteEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      previous.map { it.createdDate.toString() }.minus(current.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val p = previous.first { it.createdDate.toString() == createdDate }


          // TODO: Remove this log
          log.info("REMOVE NOTES")
          log.info(context.notesCollectionUuids[p.createdDate.toString()])

          val removeNoteCommand = RemoveCollectionItemCommand(
            user = UserDetails.from(currentVersion.updatedBy),
            assessmentUuid = context.assessmentUuid,
            collectionItemUuid = context.createdNotesUuids[p.createdDate.toString()],
          )

          commands.add(removeNoteCommand)
        }
      commands
    }

    val notesToAdd = { previous: Set<GoalNoteEntity>, current: Set<GoalNoteEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.minus(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { it.createdDate.toString() == createdDate }

          commands.add(
            AddCollectionItemCommand(
              collection = goalsToBeAdded.firstOrNull {
                it is CreateCollectionCommand
                  && it.createdOnTimestamp.toString() == c.goal?.createdDate.toString()
                  && it.name == "NOTES"
              } as? CreateCollectionCommand,
              collectionUuid = context.notesCollectionUuids[c.goal?.createdDate.toString()],
              answers = mapOf(
                "note" to SingleValue(c.note),
                "created_by" to SingleValue(c.createdBy?.username ?: "Unknown"),
              ),
              properties = mapOf(
                "created_at" to SingleValue(c.createdDate.toString()),
                "type" to SingleValue(GoalNoteTypeMapper.map(c.type)),
              ),
              index = null,
              user = UserDetails.from(c.createdBy),
              assessmentUuid = context.assessmentUuid,
              createdOnTimestamp = c.createdDate,
            ),
          )
        }
      commands
    }

    val notesToUpdate = { previous: Set<GoalNoteEntity>, current: Set<GoalNoteEntity> ->
      val commands: MutableList<Requestable> = mutableListOf()
      current.map { it.createdDate.toString() }.intersect(previous.map { it.createdDate.toString() }.toSet())
        .map { createdDate ->
          val c = current.first { it.createdDate.toString() == createdDate }
          val p = previous.first { it.createdDate.toString() == createdDate }

          val answersUpdated = buildMap {
            if (c.note != p.note) put("note", SingleValue(c.note))
            if (c.createdBy?.username != p.createdBy?.username) put(
              "created_by",
              SingleValue(c.createdBy?.username ?: "Unknown"),
            )
          }

          if (answersUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemAnswersCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = answersUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdNotesUuids[c.createdDate.toString()],
              ),
            )
          }

          val propertiesUpdated = buildMap {
            if (c.createdDate.toString() != p.createdDate.toString()) {
              put("created_at", SingleValue(c.createdDate.toString()))
            }
            if (c.type != p.type) {
              put("type", SingleValue(GoalNoteTypeMapper.map(c.type)))
            }
          }

          if (propertiesUpdated.isNotEmpty()) {
            commands.add(
              UpdateCollectionItemPropertiesCommand(
                user = UserDetails.from(c.createdBy),
                assessmentUuid = context.assessmentUuid,
                added = propertiesUpdated,
                removed = emptyList(),
                collectionItemUuid = context.createdNotesUuids[c.createdDate.toString()],
              ),
            )
          }
        }
      commands
    }

    val previousGoalsByCreatedDate = context.previousGoals.associateBy { it.createdDate.toString() }
    val currentGoalsByCreatedDate = currentVersion.goals.associateBy { it.createdDate.toString() }

    return listOf(
      *goalsToRemove(context.previousGoals, currentVersion.goals).toTypedArray(),
      *goalsToBeAdded,
      *goalsToUpdate(context.previousGoals, currentVersion.goals).toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        stepsToRemove(
          previousGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
        )
      }.toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        stepsToAdd(
          previousGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
        )
      }.toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        stepsToUpdate(
          previousGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.steps?.toSet().orEmpty(),
        )
      }.toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        notesToRemove(
          previousGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
        )
      }.toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        notesToAdd(
          previousGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
        )
      }.toTypedArray(),
      *currentGoalsByCreatedDate.keys.flatMap { timestamp ->
        notesToUpdate(
          previousGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
          currentGoalsByCreatedDate[timestamp]?.notes?.toSet().orEmpty(),
        )
      }.toTypedArray(),
    ).also { context.previousGoals = currentVersion.goals }
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
