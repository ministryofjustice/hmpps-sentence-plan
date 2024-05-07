package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.client.ARNSRestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment

@Service
class ARNSApiService(
  val arnsRestClient: ARNSRestClient,
) {
  fun getRoshInfoByCrn(crn: String): RiskAssessment? {
    return arnsRestClient.getRoshInfoByCrn(crn)
  }
}
