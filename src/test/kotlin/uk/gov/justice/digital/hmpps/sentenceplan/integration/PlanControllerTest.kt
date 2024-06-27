package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.*

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Plan Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  var plan: PlanEntity? = null

  @BeforeAll
  fun setup() {
    plan = planRepository.findAll().first()
  }

  @Test
  fun `get plan by existing UUID should return OK`() {
    val planUuid = plan?.uuid
    webTestClient.get().uri("/plans/$planUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get plan by non-existent UUID should return not found`() {
    val planUuid = UUID.randomUUID()
    webTestClient.get().uri("/plans/$planUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }
}
