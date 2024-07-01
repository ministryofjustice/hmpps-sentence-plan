package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.*

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val goalRepository: GoalRepository,
) {

  fun getPlanByUuid(planUuid: UUID): PlanEntity? = planRepository.findByUuid(planUuid)

  fun getGoalsByPlanUuid(planUuid: UUID): List<GoalEntity> = goalRepository.findByPlanUuid(planUuid)

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? = planRepository.findByOasysAssessmentPk(oasysAssessmentPk)

  fun createNewGoal(planUuid: UUID, goal: GoalEntity): GoalEntity {
    goal.planUuid = planUuid
    return goalRepository.save(goal)
  }
}
