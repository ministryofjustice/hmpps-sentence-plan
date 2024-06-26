package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.*

@RestController
@RequestMapping("/oasys")
class OasysController(private val service: PlanService) {

  @GetMapping("/{oasysAssessmentPk}")
  fun getPlan(
    @PathVariable oasysAssessmentPk: String,
  ): PlanEntity {
    return service.getPlanByOasysAssessmentPk(oasysAssessmentPk)
  }
}
