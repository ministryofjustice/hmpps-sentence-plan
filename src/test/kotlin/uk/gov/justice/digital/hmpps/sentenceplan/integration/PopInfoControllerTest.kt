package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.sentenceplan.data.CRNLinkedRequest

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("PopInfo Tests")
class PopInfoControllerTest : IntegrationTestBase() {

  private val requestBody = CRNLinkedRequest("XYZ12345")

  @Test
  fun `get pop info should return success`() {
    webTestClient.post().uri("/info/pop")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get pop info should return unauthorized when no auth token`() {
    webTestClient.post().uri("/info/pop")
      .header("Content-Type", "application/json")
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get pop info should return forbidden when no role`() {
    webTestClient.post().uri("/info/pop")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get pop info scores risk should return success`() {
    webTestClient.post().uri("/info/pop/scores/risk")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get pop info scores risk should return unauthorized when no auth token`() {
    webTestClient.post().uri("/info/pop/scores/risk")
      .header("Content-Type", "application/json")
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get pop info scores risk should return forbidden when no role`() {
    webTestClient.post().uri("/info/pop/scores/risk")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isForbidden
  }
}
