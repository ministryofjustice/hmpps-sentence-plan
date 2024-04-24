package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  fun createNewGoal(goal: GoalEntity): GoalEntity {
    return goalRepository.save(goal)
  }

  @Transactional
  fun createNewStep(steps: List<StepEntity>, goalId: UUID): List<StepEntity> {
    for (step in steps) {
      step.relatedGoalId = goalId
    }
    return stepRepository.saveAll(steps)
  }
}
