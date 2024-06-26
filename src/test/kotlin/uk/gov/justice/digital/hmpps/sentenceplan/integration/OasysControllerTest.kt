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

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Oasys Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OasysControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  var plan: PlanEntity? = null

  @BeforeAll
  fun setup() {
    // plan = planRepository.findAll().first()
  }

  @Test
  fun `get plan by existing Oasys Assessment PK should return OK`() {
    val oasysAssessmentPk = "1"
    webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get plan by non-existent Oasys Assessment PK should return not found`() {
    val oasysAssessmentPk = "2"
    webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }
}
