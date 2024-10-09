package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.CreatePlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.LockPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.CountersigningStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.getPlan
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanState
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanVersionResponse
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Coordinator Controller Tests")
class CoordinatorControllerTest : IntegrationTestBase() {

  val authenticatedUser = "OASYS|Tom C"
  val userDetails = UserDetails("1", "Tom C")

  @Autowired
  lateinit var planRepository: PlanRepository

  @Autowired
  lateinit var planVersionRepository: PlanVersionRepository

  @Nested
  @DisplayName("createPlan")
  inner class CreatePlan {
    @ParameterizedTest
    @EnumSource(PlanType::class)
    fun `should create a new plan`(planType: PlanType) {
      val createPlanRequest = CreatePlanRequest(
        planType = planType,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan")
        .bodyValue(createPlanRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isCreated
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planVersion).isEqualTo(0L)
          assertThat(responseBody?.planId).isNotNull
        }
    }
  }

  @Nested
  @DisplayName("getPlan")
  @Sql(scripts = [ "/db/test/oasys_assessment_pk_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/oasys_assessment_pk_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
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
        .returnResult().run {
          assertThat(responseBody?.sentencePlanId).isEqualTo(UUID.fromString("9f2aaa46-e544-4bcd-8db6-fbe7842ddb64"))
          assertThat(responseBody?.sentencePlanVersion).isEqualTo(0L)
          assertThat(responseBody?.planComplete).isEqualTo(PlanState.INCOMPLETE)
          assertThat(responseBody?.planType).isEqualTo(PlanType.INITIAL)
        }
    }

    @Test
    fun `should return not found when getting plan by non-existent UUID`() {
      webTestClient.get()
        .uri("/coordinator/plan/15285be5-fe67-448f-b8b0-45c9e4c7ad8e")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `should return server error when trying to get a plan param that is not a UUID`() {
      webTestClient.get()
        .uri("/coordinator/plan/x")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("signPlan")
  inner class SignPlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    val userDetails = UserDetails("1", "Tom C")

    @Sql(scripts = [ "/db/test/oasys_assessment_pk_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = [ "/db/test/oasys_assessment_pk_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
    @ParameterizedTest
    @EnumSource(SignType::class)
    fun `should do something`(signType: SignType) {
      val signRequest = SignRequest(
        signType = signType,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/sign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(0L)
        }
    }

    @Test
    fun `should return 404 not found`() {
      val signRequest = SignRequest(
        signType = SignType.COUNTERSIGN,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/sign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).startsWith("No resource found failure")
          assertThat(responseBody?.developerMessage).startsWith("No static resource")
        }
    }
  }

  @Nested
  @DisplayName("lockPlan")
  inner class LockPlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    val userDetails = UserDetails("1", "Tom C")

    @Sql(scripts = [ "/db/test/oasys_assessment_pk_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = [ "/db/test/oasys_assessment_pk_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should lock the plan and return a new version`() {
      val lockRequest = LockPlanRequest(
        userDetails = userDetails,
      )

      val beforePlanVersion = planRepository.getPlan(planUuid).currentVersion
      val beforeVersionStatus = beforePlanVersion?.status!!
      val beforeVersion = beforePlanVersion.version

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/lock")
        .bodyValue(lockRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(beforeVersion.toLong())
        }

      val afterStatus = planVersionRepository.findByPlanUuidAndVersion(planUuid, beforeVersion).status
      val newPlanVersion = planRepository.getPlan(planUuid).currentVersion

      assertThat(afterStatus).isNotEqualTo(beforeVersionStatus)
      assertThat(afterStatus).isEqualTo(CountersigningStatus.LOCKED_INCOMPLETE)
      assertThat(newPlanVersion?.version).isEqualTo(beforeVersion + 1)
      assertThat(newPlanVersion?.status).isEqualTo(CountersigningStatus.UNSIGNED)
    }

    @Test
    fun `should return 404 not found`() {
      val signRequest = SignRequest(
        signType = SignType.COUNTERSIGN,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/lock")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan not found for id 0d0f2d85-5b70-4916-9f89-ed248f8d5196")
        }
    }
  }
}
