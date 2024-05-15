package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.CRNLinkedRequest

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Reference data Tests")
class ReferenceDataControllerTest : IntegrationTestBase() {

  private val requestBody = CRNLinkedRequest("XYZ12345")

  @Test
  fun `get question reference data should return success`() {
    webTestClient.get().uri("/question-reference-data")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get question reference data should return unauthorized when no auth token`() {
    webTestClient.get().uri("/question-reference-data")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get question reference data should return forbidden when no role`() {
    webTestClient.get().uri("/question-reference-data")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }
}
