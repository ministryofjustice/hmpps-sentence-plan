package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val stepRepository: StepRepository,
) {

  fun createNewGoal(goal: GoalEntity): GoalEntity = goalRepository.save(goal)

  @Transactional
  fun createNewStep(steps: List<StepEntity>, goalUuid: UUID): List<StepEntity> {
    for (step in steps) {
      step.relatedGoalId = goalUuid
    }
    return stepRepository.saveAll(steps)
  }

  fun getAllGoals(): List<GoalEntity> = goalRepository.findAll()

  fun getAllGoalSteps(goalUuid: UUID): List<StepEntity> = stepRepository.findAllByRelatedGoalId(goalUuid).get()

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }
}
