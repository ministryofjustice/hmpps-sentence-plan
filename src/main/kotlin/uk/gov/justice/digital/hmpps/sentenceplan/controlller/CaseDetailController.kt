package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.NameResponse
import uk.gov.justice.digital.hmpps.sentenceplan.service.CaseDetailService

@RestController
class CaseDetailController(private val caseDetailService: CaseDetailService) {

  @GetMapping("/name/{crn}")
  fun getName(
    @Parameter(description = "CRN", required = true, example = "D1974X")
    @PathVariable
    crn: String,
  ): NameResponse {
    return caseDetailService.getCaseDetail(crn)
  }
}
