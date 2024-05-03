package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.client.DeliusRestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.Name

@Service
class CaseDetailService(private val deliusRestClient: DeliusRestClient) {
  fun getCaseDetail(crn: String): Name {
    return deliusRestClient.getCaseDetail(crn)?.name ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  }
}
