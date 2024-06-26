package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.*

@Service
class PlanService(
  private val planRepository: PlanRepository,
) {

  fun getPlanByUuid(planUuid: UUID): PlanEntity {
    return planRepository.findByUuid(planUuid) ?: throw EmptyResultDataAccessException(1)
  }

  // TODO does this really only expect a single response? should both of these methods be returning a List?
  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity {
    return planRepository.findByOasysAssessmentPk(oasysAssessmentPk) ?: throw EmptyResultDataAccessException(1)
  }
}
