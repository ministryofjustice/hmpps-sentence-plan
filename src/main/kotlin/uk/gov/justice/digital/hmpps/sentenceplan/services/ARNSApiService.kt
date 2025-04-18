package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.CaseDetail
import uk.gov.justice.digital.hmpps.sentenceplan.data.PopInfoResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessmentResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCommunityResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskInCustodyResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.ScoreEnum

@Service
class ARNSApiService(
  @Qualifier("arnsRestClient") private val arnsRestClient: WebClient,
  @Qualifier("deliusRestClient")private val deliusRestClient: WebClient,
) {

  fun getPopInfo(crn: String): PopInfoResponse {
    val caseDetail: CaseDetail? =
      deliusRestClient.get()
        .uri("/case-details/$crn")
        .retrieve()
        .bodyToMono(CaseDetail::class.java)
        .onErrorResume(WebClientResponseException.NotFound::class.java) { throw ResponseStatusException(HttpStatus.NOT_FOUND) }
        .block()

    // block() can return null if the mono is empty
    if (caseDetail == null) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    return PopInfoResponse(
      "Miss",
      caseDetail.name?.forename,
      caseDetail.name?.surname,
      "Gender.female",
      caseDetail.dateOfBirth,
      caseDetail.crn,
      "ABC123XYZ",
      mapOf<String, Any>(),
      caseDetail.sentences,
    )
  }

  fun getRiskScoreInfoByCrn(crn: String): RiskAssessmentResponse {
    val riskAssessment: RiskAssessment? =
      arnsRestClient.get()
        .uri("/risks/crn/$crn")
        .retrieve()
        .bodyToMono(RiskAssessment::class.java)
        .onErrorResume(WebClientResponseException.NotFound::class.java) { throw ResponseStatusException(HttpStatus.NOT_FOUND) }
        .block()

    // block() can return null if the mono is empty
    if (riskAssessment == null) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    val riskInCommunityMap = LinkedHashMap<String, ScoreEnum>()
    val riskInCustodyMap = LinkedHashMap<String, ScoreEnum>()

    riskAssessment.summary.riskInCommunity[SCORE_VERY_HIGH]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.VERY_HIGH }
    riskAssessment.summary.riskInCommunity[SCORE_HIGH]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCommunity[SCORE_MEDIUM]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.MEDIUM }
    riskAssessment.summary.riskInCommunity[SCORE_LOW]?.forEach { risk -> riskInCommunityMap[risk] = ScoreEnum.LOW }

    riskAssessment.summary.riskInCustody[SCORE_HIGH]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.HIGH }
    riskAssessment.summary.riskInCustody[SCORE_VERY_HIGH]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.VERY_HIGH }
    riskAssessment.summary.riskInCustody[SCORE_MEDIUM]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.MEDIUM }
    riskAssessment.summary.riskInCustody[SCORE_LOW]?.forEach { risk -> riskInCustodyMap[risk] = ScoreEnum.LOW }

    val riskCommunity = RiskInCommunityResponse(
      riskInCommunityMap[COMMUNITY_PUBLIC] ?: ScoreEnum.LOW,
      riskInCommunityMap[COMMUNITY_CHILDREN] ?: ScoreEnum.LOW,
      riskInCommunityMap[COMMUNITY_KNOW_ADULT] ?: ScoreEnum.LOW,
      riskInCommunityMap[COMMUNITY_STAFF] ?: ScoreEnum.LOW,
    )
    val riskCustody = RiskInCustodyResponse(
      riskInCustodyMap[COMMUNITY_PUBLIC] ?: ScoreEnum.LOW,
      riskInCustodyMap[COMMUNITY_CHILDREN] ?: ScoreEnum.LOW,
      riskInCustodyMap[COMMUNITY_KNOW_ADULT] ?: ScoreEnum.LOW,
      riskInCustodyMap[COMMUNITY_STAFF] ?: ScoreEnum.LOW,
      riskInCustodyMap[COMMUNITY_PRISONERS] ?: ScoreEnum.LOW,
    )

    return RiskAssessmentResponse(
      ScoreEnum.valueOf(riskAssessment.summary.overallRiskLevel),
      riskAssessment.assessedOn,
      riskCommunity,
      riskCustody,
    )
  }
  private companion object {
    private const val SCORE_HIGH = "HIGH"
    private const val SCORE_LOW = "LOW"
    private const val SCORE_MEDIUM = "MEDIUM"
    private const val SCORE_VERY_HIGH = "VERY_HIGH"
    private const val COMMUNITY_PUBLIC = "Public"
    private const val COMMUNITY_CHILDREN = "Children"
    private const val COMMUNITY_KNOW_ADULT = "Known Adult"
    private const val COMMUNITY_STAFF = "Staff"
    private const val COMMUNITY_PRISONERS = "Prisoners"
  }
}
