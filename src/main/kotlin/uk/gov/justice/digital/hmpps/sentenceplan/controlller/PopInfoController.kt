package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.CRNLinkedRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.PopInfoResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessmentResponse
import uk.gov.justice.digital.hmpps.sentenceplan.services.ARNSApiService

@RestController
@RequestMapping("/info/pop")
@PreAuthorize("hasAnyRole('ROLE_RISK_INTEGRATIONS_RO')")
class PopInfoController(
  private val arnsApiService: ARNSApiService,
) {
  @PostMapping
  fun getPopInfo(
    @RequestBody body: CRNLinkedRequest,
  ): PopInfoResponse {
    val crn = body.crn
    return arnsApiService.getPopInfo(crn)
  }

  @PostMapping("/scores/risk")
  fun getRiskScore(
    @RequestBody body: CRNLinkedRequest,
  ): RiskAssessmentResponse? {
    val crn = body.crn
    return arnsApiService.getRiskScoreInfoByCrn(crn)
  }
}
