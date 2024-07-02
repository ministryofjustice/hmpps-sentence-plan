package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.UUID

@Service
class PlanService(
  private val planRepository: PlanRepository,
) {

  fun getPlanByUuid(planUuid: UUID): PlanEntity? = planRepository.findByUuid(planUuid)

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? =
    planRepository.findByOasysAssessmentPk(oasysAssessmentPk)

  fun createPlan(oasysAssessmentPk: String): PlanEntity {
    // if a plan already exists for this PK don't add one - throw already exists
    if (getPlanByOasysAssessmentPk(oasysAssessmentPk) == null) {
      val plan: PlanEntity = planRepository.save(PlanEntity())
      planRepository.createOasysAssessmentPk(oasysAssessmentPk, plan.uuid)
      return plan
    } else {
      // throw an exception up the stack?
      return PlanEntity() // placeholder so I can commit
    }
  }
}
