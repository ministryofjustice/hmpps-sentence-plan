package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.client.ARNSRestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCommunityResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCustodyResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.ScoreEnum

@Service
class ARNSApiService(
  val arnsRestClient: ARNSRestClient,
) {
  fun getRoshInfoByCrn(crn: String): RiskResponse {
    val riskAssessment: RiskAssessment = arnsRestClient.getRoshInfoByCrn(crn) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val riskInCommunityMap = LinkedHashMap<String, ScoreEnum>()
    val riskInCustodyMap = LinkedHashMap<String, ScoreEnum>()

    riskAssessment.summary.riskInCommunity["HIGH"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCommunity["VERYHIGH"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.VERY_HIGH }
    riskAssessment.summary.riskInCommunity["LOW"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.LOW }
    riskAssessment.summary.riskInCommunity["MEDIUM"]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.MEDIUM }

    riskAssessment.summary.riskInCustody["HIGH"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCustody["VERYHIGH"]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.VERY_HIGH }
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
}
