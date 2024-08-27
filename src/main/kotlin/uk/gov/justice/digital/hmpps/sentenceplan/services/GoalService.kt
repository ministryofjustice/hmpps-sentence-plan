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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
  private val planRepository: PlanRepository,
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
      ?: throw Exception("This Goal is not found: $goalUuid")

    goalEntity.title = goal.title
    goalEntity.targetDate = goal.targetDate

    var relatedAreasOfNeedEntity: List<AreaOfNeedEntity> = emptyList()

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
  fun createNewSteps(goalUuid: UUID, steps: List<Step>): List<StepEntity> {
    val goal: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal is not found: $goalUuid")

    if(steps.isNotEmpty()) {
      checkStepsAreValid(steps)
    }

    goal.steps = createStepEntitiesFromSteps(goal, steps)
    return goalRepository.save(goal).steps
  }

  @Transactional
  fun updateSteps(goalUuid: UUID, steps: List<Step>): List<StepEntity>? {
    var goalEntity: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal is not found: $goalUuid")

    if (steps.isEmpty()) {
      throw IllegalArgumentException("At least one Step must be provided")
    }

    checkStepsAreValid(steps)

    goalEntity.steps = createStepEntitiesFromSteps(goalEntity, steps)
    goalEntity = goalRepository.save(goalEntity)
    return goalEntity.steps
  }

  private fun checkStepsAreValid(steps: List<Step>) {
    steps.forEach(
      { step ->
        if (step.description.isEmpty() || step.actor.isEmpty() || step.status.isEmpty()) {
          throw IllegalArgumentException("All Steps must contain all the required information")
        }
      },
    )
  }

  private fun createStepEntitiesFromSteps(
    goal: GoalEntity,
    steps: List<Step>,
  ): ArrayList<StepEntity> {
    val stepEntityList = ArrayList<StepEntity>()
    steps.forEach { step ->
      val stepEntity = StepEntity(
        description = step.description,
        status = step.status,
        goal = goal,
        actor = step.actor,
      )
      stepEntityList.add(stepEntity)
    }
    return stepEntityList
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
