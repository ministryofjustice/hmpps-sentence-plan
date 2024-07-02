package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import java.util.UUID

private const val TEST_DATA_STEP_UUID = "71793b64-545e-4ae7-9936-610639093857"

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Plan Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StepControllerTest : IntegrationTestBase() {

  @Test
  fun `get step by existing UUID should return OK`() {
    webTestClient.get().uri("/steps/${TEST_DATA_STEP_UUID}")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get step by non-existent UUID should return not found`() {
    val randomStepUuid = UUID.randomUUID()
    webTestClient.get().uri("/steps/$randomStepUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }
}
