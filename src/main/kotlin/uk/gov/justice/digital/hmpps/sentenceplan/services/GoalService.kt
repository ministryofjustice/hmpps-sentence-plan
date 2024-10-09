package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.dao.EmptyResultDataAccessException
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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
  private val stepRepository: StepRepository,
  private val planRepository: PlanRepository,
  private val versionService: VersionService,
) {
  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val planVersionEntity: PlanVersionEntity

    try {
      planVersionEntity = planRepository.findByUuid(planUuid).currentVersion!!
    } catch (e: EmptyResultDataAccessException) {
      throw Exception("A Plan with this UUID was not found: $planUuid")
    }

    require(goal.areaOfNeed != null && goal.title != null)

    val areaOfNeedEntity: AreaOfNeedEntity

    try {
      areaOfNeedEntity = areaOfNeedRepository.findByNameIgnoreCase(goal.areaOfNeed)
    } catch (e: EmptyResultDataAccessException) {
      throw Exception("An Area of Need with this name was not found: ${goal.areaOfNeed}")
    }

    val relatedAreasOfNeedEntity = getAreasOfNeedByNames(goal)

    val highestGoalOrder = planVersionEntity.goals.maxByOrNull { g -> g.goalOrder }?.goalOrder ?: 0

    val currentPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersionEntity)

    val goalEntity = GoalEntity(
      title = goal.title,
      areaOfNeed = areaOfNeedEntity,
      targetDate = goal.targetDate?.let { LocalDate.parse(it) },
      status = if (goal.targetDate != null) GoalStatus.ACTIVE else GoalStatus.FUTURE,
      statusDate = LocalDateTime.now(),
      planVersion = currentPlanVersion,
      relatedAreasOfNeed = relatedAreasOfNeedEntity.toMutableSet(),
      goalOrder = highestGoalOrder + 1,
    )
    val savedGoalEntity = goalRepository.save(goalEntity)

    return savedGoalEntity
  }

  private fun getAreasOfNeedByNames(goal: Goal): List<AreaOfNeedEntity> {
    var relatedAreasOfNeedList: List<AreaOfNeedEntity> = emptyList()
    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedList = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      // findAllByNames doesn't throw an exception if a subset of goal.relatedAreasOfNeed is not found, so we
      // do a hard check on the count of returned items here
      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedList.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }
    return relatedAreasOfNeedList
  }

  @Transactional
  fun updateGoalByUuid(goalUuid: UUID, goal: Goal): GoalEntity {
    val goalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    val relatedAreasOfNeedList: List<AreaOfNeedEntity> = getAreasOfNeedByNames(goal)
    goalEntity.merge(goal, relatedAreasOfNeedList)

    versionService.conditionallyCreateNewPlanVersion(goalEntity.planVersion)

    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun addStepsToGoal(goalUuid: UUID, steps: List<Step>, replaceExistingSteps: Boolean = false): List<StepEntity> {
    var goal: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    require(steps.isNotEmpty()) { "At least one Step must be provided" }

    requireStepsAreValid(steps)

    val planVersion = goal.planVersion
    versionService.conditionallyCreateNewPlanVersion(planVersion)

    // the planversion has changed, so refetch the goal to make sure we have the right version tree
    goal = goalRepository.findByUuid(goalUuid)!!

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
  ): List<StepEntity> = steps.map {
    StepEntity(
      description = it.description,
      status = it.status,
      goal = goal,
      actor = it.actor,
    )
  }

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }

  @Transactional
  fun deleteGoalByUuid(goalUuid: UUID): Int {
    versionService.conditionallyCreateNewPlanVersion(goalRepository.findByUuid(goalUuid)?.planVersion)
    return goalRepository.deleteByUuid(goalUuid)
  }
}
