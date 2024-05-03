package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.CRNLinkedRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment
import uk.gov.justice.digital.hmpps.sentenceplan.service.ARNSApiService

@RestController
@RequestMapping("/info/pop")
class PopInfoController(
  private val arnsApiService: ARNSApiService,
) {
  @PostMapping("/scores/risk")
  fun getRiskScore(
    @RequestBody body: CRNLinkedRequest,
  ): RiskAssessment? {
    val crn = body.crn
    return arnsApiService.getRoshInfoByCrn(crn)
  }
}
