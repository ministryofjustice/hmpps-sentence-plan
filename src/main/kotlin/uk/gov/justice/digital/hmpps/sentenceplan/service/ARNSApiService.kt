package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.client.ARNSRestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCommunityResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCustodyResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.ScoreEnum
import uk.gov.justice.digital.hmpps.sentenceplan.stub.StubData

@Service
class ARNSApiService(
  val arnsRestClient: ARNSRestClient,
  @Value("\${use-stub}") private val useStub: Boolean,
) {
  fun getRiskScoreInfoByCrn(crn: String): RiskResponse {
    val riskAssessment: RiskAssessment =
      if (useStub) {
        log.info("Calling Stub")
        StubData.getRiskScoreInfoByCrn(crn)
      } else {
        log.info("Calling ARNSRestClient")
        arnsRestClient.getRiskScoreInfoByCrn(crn) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
      }
    val riskInCommunityMap = LinkedHashMap<String, ScoreEnum>()
    val riskInCustodyMap = LinkedHashMap<String, ScoreEnum>()

    riskAssessment.summary.riskInCommunity["HIGH"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCommunity["VERY_HIGH"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.VERY_HIGH }
    riskAssessment.summary.riskInCommunity["LOW"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.LOW }
    riskAssessment.summary.riskInCommunity["MEDIUM"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.MEDIUM }

    riskAssessment.summary.riskInCustody["HIGH"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCustody["VERY_HIGH"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.VERY_HIGH }
    riskAssessment.summary.riskInCustody["LOW"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.LOW }
    riskAssessment.summary.riskInCustody["MEDIUM"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.MEDIUM }

    val riskCommunity = RiskInCommunityResponse(
      riskInCommunityMap["Public"] ?: ScoreEnum.LOW,
      riskInCommunityMap["Children"] ?: ScoreEnum.LOW,
      riskInCommunityMap["Know adult"] ?: ScoreEnum.LOW,
      riskInCommunityMap["Staff"] ?: ScoreEnum.LOW,
    )
    val riskCustody = RiskInCustodyResponse(
      riskInCustodyMap["Public"] ?: ScoreEnum.LOW,
      riskInCustodyMap["Children"] ?: ScoreEnum.LOW,
      riskInCustodyMap["Know adult"] ?: ScoreEnum.LOW,
      riskInCustodyMap["Staff"] ?: ScoreEnum.LOW,
      riskInCustodyMap["Prisoners"] ?: ScoreEnum.LOW,
    )

    return RiskResponse(
      ScoreEnum.valueOf(riskAssessment.summary.overallRiskLevel),
      riskAssessment.assessedOn,
      riskCommunity,
      riskCustody,
    )
  }
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
