package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.sentenceplan.data.CreatePlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanState
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanVersionResponse
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Coordinator Controller Tests")
class CoordinatorControllerTest : IntegrationTestBase() {

  val authenticatedUser = "${UUID.randomUUID()}|Tom C"

  @Nested
  @DisplayName("createPlan")
  inner class CreatePlan {
    @Test
    fun `should create a new plan`() {
      val createPlanRequest = CreatePlanRequest(
        PlanType.INITIAL,
        UserDetails(
          "1",
          "Tom C",
        ),
      )

      webTestClient.post()
        .uri("/coordinator/plan")
        .bodyValue(createPlanRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isCreated
        .expectBody<PlanVersionResponse>()
        .returnResult().apply {
          assertThat(responseBody?.planVersion).isEqualTo(0L)
          assertThat(responseBody?.planUuid).isNotNull
        }
    }
  }

  @Nested
  @DisplayName("getPlan")
  inner class GetPlan {
    val staticPlanUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")

    @Test
    fun `should retrieve a plan`() {
      webTestClient.get()
        .uri("/coordinator/plan/$staticPlanUuid")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody<GetPlanResponse>()
        .returnResult().apply {
          assertThat(responseBody?.sentencePlanId).isEqualTo(staticPlanUuid)
          assertThat(responseBody?.sentencePlanVersion).isEqualTo(1L)
          assertThat(responseBody?.planComplete).isEqualTo(PlanState.INCOMPLETE)
          assertThat(responseBody?.planType).isEqualTo(PlanType.INITIAL)
        }
    }
  }
}
