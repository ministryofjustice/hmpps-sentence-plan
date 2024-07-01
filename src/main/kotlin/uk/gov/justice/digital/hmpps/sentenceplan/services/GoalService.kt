package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.*

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val stepRepository: StepRepository,
) {

  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewStep(goalUuid: UUID, steps: List<StepEntity>): List<StepEntity> {
    val stepsRelatedToGoal: List<StepEntity> = addRelatedGoalUuidToSteps(goalUuid, steps)
    return stepRepository.saveAll(stepsRelatedToGoal)
  }

  internal fun addRelatedGoalUuidToSteps(
    goalUuid: UUID,
    steps: List<StepEntity>,
  ): List<StepEntity> {
    return steps.onEach { it -> it.relatedGoalUuid = goalUuid }
  }

  fun getAllGoals(): List<GoalEntity> = goalRepository.findAll()

  fun getAllGoalSteps(goalUuid: UUID): List<StepEntity> = stepRepository.findByRelatedGoalUuid(goalUuid)

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }
}
