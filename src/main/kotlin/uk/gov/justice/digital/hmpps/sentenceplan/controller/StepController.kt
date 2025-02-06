package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.StepService
import java.util.UUID

@RestController
@RequestMapping("/steps")
class StepController(private val service: StepService) {

  @GetMapping("/{stepUuid}")
  fun getStep(
    @PathVariable stepUuid: UUID,
  ): StepEntity = service.getStepByUuid(stepUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No step found for $stepUuid")
}
