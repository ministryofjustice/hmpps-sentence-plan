package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.*

@Service
class PlanService(
  private val planRepository: PlanRepository,
) {

  fun getPlanByUuid(planUuid: UUID): PlanEntity? = planRepository.findByUuid(planUuid)

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? = planRepository.findByOasysAssessmentPk(oasysAssessmentPk)
}
