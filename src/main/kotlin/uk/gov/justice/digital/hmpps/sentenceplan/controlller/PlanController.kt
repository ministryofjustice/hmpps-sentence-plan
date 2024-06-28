package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.*

@RestController
@RequestMapping("/plans")
class PlanController(private val service: PlanService) {

  @GetMapping("/{planUuid}")
  fun getPlan(
    @PathVariable planUuid: UUID,
  ): PlanEntity {
    return service.getPlanByUuid(planUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No resource found for $planUuid")
  }
}