package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Oasys Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OasysControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("getPlan")
  inner class GetPlan {
    @Test
    fun `get plan by existing Oasys Assessment PK should return OK`() {
      val oasysAssessmentPk = "1"
      webTestClient.get().uri("/oasys/plans/$oasysAssessmentPk")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `get plan by non-existent Oasys Assessment PK should return not found`() {
      val oasysAssessmentPk = "2"
      webTestClient.get().uri("/oasys/plans/$oasysAssessmentPk")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
