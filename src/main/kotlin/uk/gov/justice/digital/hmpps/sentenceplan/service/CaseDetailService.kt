package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.client.DeliusRestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.Name
import uk.gov.justice.digital.hmpps.sentenceplan.data.NameResponse
import uk.gov.justice.digital.hmpps.sentenceplan.stub.StubData

@Service
class CaseDetailService(
  private val deliusRestClient: DeliusRestClient,
  @Value("\${use-stub}") private val useStub: Boolean,
) {
  fun getCaseDetail(crn: String): NameResponse {
    val name: Name =
      if (useStub) {
        log.info("Calling Stub")
        StubData.getCaseDetail(crn).name ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
      } else {
        log.info("Calling DeliusRestClient")
        deliusRestClient.getCaseDetail(crn)?.name ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
      }
    return NameResponse(name.forename, name.surname)
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
