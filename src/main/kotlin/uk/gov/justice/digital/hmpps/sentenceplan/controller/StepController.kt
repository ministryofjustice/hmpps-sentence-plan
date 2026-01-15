package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.services.StepService
import java.util.UUID

@RestController
@RequestMapping("/steps")
class StepController(private val service: StepService) {

  @GetMapping("/{stepUuid}")
  fun getStep(
    @PathVariable stepUuid: UUID,
  ) = service.getStepByUuid(stepUuid) ?: throw NotFoundException("No step found for $stepUuid")
}
