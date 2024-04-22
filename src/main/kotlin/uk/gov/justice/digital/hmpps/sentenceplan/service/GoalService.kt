package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val stepRepository: StepRepository,
) {
  fun createNewGoal(goal: GoalEntity): GoalEntity {
    return goalRepository.save(goal)
  }

  fun createNewStep(step: StepEntity): StepEntity {
    return stepRepository.save(step)
  }
}
