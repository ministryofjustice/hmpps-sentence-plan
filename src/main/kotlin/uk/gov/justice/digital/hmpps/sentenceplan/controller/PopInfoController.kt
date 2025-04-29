package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.CRNLinkedRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.PopInfoResponse
import uk.gov.justice.digital.hmpps.sentenceplan.services.DeliusApiService

@RestController
@RequestMapping("/info/pop")
@PreAuthorize("hasAnyRole('ROLE_RISK_INTEGRATIONS_RO')")
class PopInfoController(
  private val deliusApiService: DeliusApiService,
) {
  @PostMapping
  fun getPopInfo(
    @RequestBody body: CRNLinkedRequest,
  ): PopInfoResponse {
    val crn = body.crn
    return deliusApiService.getPopInfo(crn)
  }
}
