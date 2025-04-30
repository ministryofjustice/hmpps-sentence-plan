package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.CaseDetail
import uk.gov.justice.digital.hmpps.sentenceplan.data.PopInfoResponse

@Service
class DeliusApiService(
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
}
