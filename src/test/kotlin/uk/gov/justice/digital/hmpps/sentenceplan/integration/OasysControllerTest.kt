package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.CreatePlanWithOasysAssesmentPkRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Oasys Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OasysControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository
  val authenticatedUser = UUID.randomUUID().toString() + "|Tom C"

  @Nested
  @DisplayName("createPlan")
  inner class CreatePlan {
    lateinit var planRequestBody: CreatePlanWithOasysAssesmentPkRequest

    @BeforeEach
    fun setup() {
      planRequestBody = CreatePlanWithOasysAssesmentPkRequest(
        oasysAssessmentPk = (0..999_999_999_999).random().toString(),
      )
    }

    @Test
    fun `should return created`() {
      webTestClient.post().uri("/oasys/plans").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(planRequestBody)
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    @WithMockUser(username = "SYSTEM|OASTUB")
    fun `should return conflict when oasys_assessment_PK has existing association`() {
      val planVersionEntity = planRepository.save(PlanEntity())
      planRepository.createOasysAssessmentPk(planRequestBody.oasysAssessmentPk, planVersionEntity.id!!)

      val response = webTestClient.post().uri("/oasys/plans").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(planRequestBody)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody

      assertThat(response?.developerMessage).contains("Plan already associated with PK: ${planRequestBody.oasysAssessmentPk}")
    }
  }

  @Nested
  @DisplayName("getPlan")
  @Sql(scripts = [ "/db/test/oasys_assessment_pk_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/oasys_assessment_pk_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlan {
    @Test
    fun `get plan by existing Oasys Assessment PK should return OK`() {
      val oasysAssessmentPk = "1"
      webTestClient.get().uri("/oasys/plans/$oasysAssessmentPk")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanEntity>()
    }

    @Test
    fun `get plan by non-existent Oasys Assessment PK should return not found`() {
      val oasysAssessmentPk = "2"
      webTestClient.get().uri("/oasys/plans/$oasysAssessmentPk")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
