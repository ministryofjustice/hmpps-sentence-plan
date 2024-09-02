package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
  private val planRepository: PlanRepository,
  private val stepRepository: StepRepository,
) {

  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val planEntity = planRepository.findByUuid(planUuid)
      ?: throw Exception("A Plan with this UUID was not found: $planUuid")

    val areaOfNeedEntity = areaOfNeedRepository.findByNameIgnoreCase(goal.areaOfNeed)
      ?: throw Exception("An Area of Need with this name was not found: ${goal.areaOfNeed}")

    var relatedAreasOfNeedEntity: List<AreaOfNeedEntity> = emptyList()

    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedEntity = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedEntity.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }

    val highestGoalOrder = planEntity.goals.maxByOrNull { g -> g.goalOrder }?.goalOrder ?: 0

    val goalEntity = GoalEntity(
      title = goal.title,
      areaOfNeed = areaOfNeedEntity,
      targetDate = goal.targetDate,
      goalStatus = if (goal.targetDate != null) GoalStatus.ACTIVE else GoalStatus.FUTURE,
      statusDate = null,
      plan = planEntity,
      relatedAreasOfNeed = relatedAreasOfNeedEntity.toMutableList(),
      goalOrder = highestGoalOrder + 1,
    )
    val savedGoalEntity = goalRepository.save(goalEntity)

    return savedGoalEntity
  }

  @Transactional
  fun updateGoalByUuid(goalUuid: UUID, goal: Goal): GoalEntity {
    val goalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    goalEntity.title = goal.title
    goalEntity.targetDate = goal.targetDate
    goalEntity.goalStatus = if (goal.targetDate != null) GoalStatus.ACTIVE else GoalStatus.FUTURE

    var relatedAreasOfNeedEntity = emptyList<AreaOfNeedEntity>()

    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedEntity = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      // findAllByNames doesn't throw an exception if a subset of goal.relatedAreasOfNeed is not found, so we
      // do a hard check on the count of returned items here
      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedEntity.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }

    goalEntity.relatedAreasOfNeed = relatedAreasOfNeedEntity.toMutableList()

    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun addStepsToGoal(goalUuid: UUID, steps: List<Step>, replaceExistingSteps: Boolean = false): List<StepEntity> {
    val goal: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    require(steps.isNotEmpty()) { "At least one Step must be provided" }

    requireStepsAreValid(steps)

    if (replaceExistingSteps) {
      stepRepository.deleteAll(goal.steps)
    }

    goal.steps = createStepEntitiesFromSteps(goal, steps)

    val savedGoal: GoalEntity = goalRepository.save(goal)
    return savedGoal.steps
  }

  private fun requireStepsAreValid(steps: List<Step>) {
    steps.forEach {
      require(it.description.isNotEmpty() && it.actor.isNotEmpty()) {
        "All Steps must contain all the required information"
      }
    }
  }

  private fun createStepEntitiesFromSteps(
    goal: GoalEntity,
    steps: List<Step>,
  ): List<StepEntity> {
    return steps.map {
      StepEntity(
        description = it.description,
        status = it.status,
        goal = goal,
        actor = it.actor,
      )
    }
  }

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }

  @Transactional
  fun deleteGoal(goalUuid: UUID): Unit? {
    val goalEntity: GoalEntity? = goalRepository.findByUuid(goalUuid)
    return goalEntity?.let { goalRepository.delete(it) }
  }
}
