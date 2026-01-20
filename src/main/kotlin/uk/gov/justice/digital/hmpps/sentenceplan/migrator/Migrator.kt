package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RequestableCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class CommandsRequest(
  val commands: List<RequestableCommand>,
)

typealias Requests = List<CommandsRequest>

data class PlanMigrationContext(val assessmentUuid: UUID) {
  lateinit var goalsCollectionUuid: UUID
}

@Component
@Profile("migration")
class Migrator(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
) : CommandLineRunner {
  fun migrate(plan: PlanEntity) {
    plan.currentVersion?.let { currentVersion ->
      val createAssessmentCommand = CreateAssessmentCommand(
        user = User.from(plan.createdBy),
        formVersion = "1",
        properties = buildMap {},
        timeline = null,
        // TODO: add timeline item
      )

      // TODO: Replace with actual call to create
      val migrationContext = PlanMigrationContext(UUID.randomUUID())

      val createGoalsCollectionCommand = CreateCollectionCommand(
        name = "GOALS",
        parentCollectionItemUuid = null,
        user = User.from(plan.createdBy),
        assessmentUuid = migrationContext.assessmentUuid,
      )

      // TODO: Replace with actual call to create collection
      migrationContext.apply { goalsCollectionUuid = UUID.randomUUID() }

      migrationContext
    }?.let { context ->
      var previousPlanVersion: PlanVersionEntity? = null
      plan.id?.run(planVersionRepository::findAllByPlanId)?.map { currentPlanVersion ->
        val removeGoalsCommands = previousPlanVersion?.goals.orEmpty()
          .filter { previous -> currentPlanVersion.goals.find { current -> current.uuid == previous.uuid } != null }
          .map { previousGoalVersion ->
            RemoveCollectionItemCommand(
              collectionItemUuid = previousGoalVersion.uuid,
              user = User.from(currentPlanVersion.updatedBy),
              assessmentUuid = context.assessmentUuid,
            )
          }
        val createAndUpdateGoalsCommands = currentPlanVersion.goals.map { currentGoalVersion ->
          val previousGoalVersion =
            previousPlanVersion?.goals.orEmpty()
              // order by which these were created/modified, this *should* ensure events are constructed in order
              .sortedBy { it.updatedDate }
              .find { it.uuid == currentGoalVersion.uuid }
          val commandsPayload = mutableListOf<RequestableCommand>()
          if (previousGoalVersion == null) {
            commandsPayload.add(
              AddCollectionItemCommand(
                collectionUuid = context.assessmentUuid,
                answers = emptyMap(),
                properties = emptyMap(),
                index = currentGoalVersion.goalOrder, // explicitly set order by what we know
                user = User.from(currentGoalVersion.createdBy),
                assessmentUuid = context.assessmentUuid,
                // TODO: add timeline item
              ),
            )
          } else {
            if (previousGoalVersion.goalOrder != currentGoalVersion.goalOrder) {
              commandsPayload.add(
                ReorderCollectionItemCommand(
                  collectionItemUuid = context.goalsCollectionUuid,
                  index = currentGoalVersion.goalOrder,
                  user = User.from(currentGoalVersion.createdBy),
                  assessmentUuid = context.assessmentUuid,
                  // TODO: add timeline item
                ),
              )
            }

            val payload: Map<String, List<String>> = buildMap {
              if (previousGoalVersion.status != currentGoalVersion.status) {
                put("GOAL_STATUS", listOf(currentGoalVersion.status.name))
              }

              // NOTE: Continue to check other fields of the goal version and add to the payload
              // Some fields may need to be treated as the "goalOrder" above and be their own command
              // if they are significant event that should have its own timeline item
            }

            commandsPayload.add(
              UpdateCollectionItemAnswersCommand(
                collectionItemUuid = context.goalsCollectionUuid,
                added = payload,
                removed = emptyList(),
                user = User.from(currentGoalVersion.createdBy),
                assessmentUuid = context.assessmentUuid,
                // TODO: add timeline item
              ),
            )
          }
          commandsPayload
        }

        val allCommands = removeGoalsCommands + createAndUpdateGoalsCommands
        // TODO: submit those commands

        previousPlanVersion = currentPlanVersion
      }
    }

    planRepository.save(plan.apply { migrated = true })
  }

  override fun run(vararg args: String?) {
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
}
