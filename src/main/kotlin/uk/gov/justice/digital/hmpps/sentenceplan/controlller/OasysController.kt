package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.data.CreatePlanWithOasysAssesmentPkRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService

@RestController
@RequestMapping("/oasys")
class OasysController(private val service: PlanService) {

  @GetMapping("/plans/{oasysAssessmentPk}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable oasysAssessmentPk: String,
  ): PlanEntity {
    return service.getPlanByOasysAssessmentPk(oasysAssessmentPk) ?: throw NoResourceFoundException(HttpMethod.GET, "No resource found for $oasysAssessmentPk")
  }

  @PostMapping("/plans")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPlan(
    @RequestBody requestBody: CreatePlanWithOasysAssesmentPkRequest,
  ): PlanEntity {
    return service.createPlanByOasysAssessmentPk(requestBody.oasysAssessmentPk)
  }
}
