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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.ClonePlanVersionRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CounterSignPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CountersignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.RestorePlanVersionsRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.RollbackPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SoftDeletePlanVersionsRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanState
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanVersionResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanVersionsResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.SoftDeletePlanVersionsResponse
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "30s")
@DisplayName("Coordinator Controller Tests")
class CoordinatorControllerTest : IntegrationTestBase() {

  val authenticatedUser = "hmpps-coordinator-api-client"
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

      val responseBody = webTestClient.post()
        .uri("/coordinator/plan")
        .bodyValue(createPlanRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isCreated
        .expectBody<PlanVersionResponse>()
        .returnResult().responseBody

      assertThat(responseBody).isNotNull
      assertThat(responseBody?.planVersion).isEqualTo(0L)
      assertThat(responseBody?.planId).isNotNull

      planRepository.getByUuid(responseBody!!.planId).let {
        assertThat(it.currentVersion?.version).isEqualTo(0)
        assertThat(it.currentVersion?.planType).isEqualTo(planType)
        assertThat(it.lastUpdatedBy?.username).isEqualTo(userDetails.name)
        assertThat(it.createdBy?.username).isEqualTo(userDetails.name)
      }
    }
  }

  @Nested
  @DisplayName("getPlan")
  @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlan {
    val staticPlanUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")

    @Test
    fun `should retrieve a plan`() {
      webTestClient.get()
        .uri("/coordinator/plan/$staticPlanUuid")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isOk
        .expectBody<GetPlanResponse>()
        .returnResult().run {
          assertThat(responseBody?.sentencePlanId).isEqualTo(UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33"))
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
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `should return server error when trying to get a plan param that is not a UUID`() {
      webTestClient.get()
        .uri("/coordinator/plan/x")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("getPlanVersions")
  @Sql(scripts = ["/db/test/plan_versions_data.sql"], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlanVersions {
    val staticPlanUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val staticPlanVersion1Uuid = UUID.fromString("353fae07-4a6d-4614-afd6-bb3953f8fadb")
    val staticPlanVersion2Uuid = UUID.fromString("59f8d10b-dda6-48d0-8f65-bb5eb1216b3b")

    @Test
    fun `should retrieve a plan's versions`() {
      webTestClient.get()
        .uri("/coordinator/plan/$staticPlanUuid/all")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionsResponse>()
        .returnResult().run {
          assertThat(responseBody?.size).isEqualTo(2)
          assertThat(responseBody?.get(0)?.uuid).isEqualTo(staticPlanVersion1Uuid)
          assertThat(responseBody?.get(0)?.version).isEqualTo(0)
          assertThat(responseBody?.get(0)?.status).isEqualTo(CountersigningStatus.UNSIGNED)
          assertThat(responseBody?.get(1)?.uuid).isEqualTo(staticPlanVersion2Uuid)
          assertThat(responseBody?.get(1)?.version).isEqualTo(2)
          assertThat(responseBody?.get(1)?.status).isEqualTo(CountersigningStatus.SELF_SIGNED)
        }
    }

    @Test
    fun `should return not found when getting plan by non-existent UUID`() {
      webTestClient.get()
        .uri("/coordinator/plan/15285be5-fe67-448f-b8b0-45c9e4c7ad8e/all")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `should return server error when trying to get a plan param that is not a UUID`() {
      webTestClient.get()
        .uri("/coordinator/plan/x/all")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
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

    @Sql(
      scripts = ["/db/test/plan_data.sql", "/db/test/oasys_assessment_pk_data_agreed.sql"],
      executionPhase = BEFORE_TEST_METHOD,
    )
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @ParameterizedTest
    @EnumSource(SignType::class)
    fun `should update the status of the plan`(signType: SignType) {
      val signRequest = SignRequest(
        signType = signType,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/sign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(0L)
        }

      planVersionRepository.getVersionByUuidAndVersion(planUuid, 0).let {
        when (signRequest.signType) {
          SignType.SELF -> {
            it.status = CountersigningStatus.SELF_SIGNED
          }

          SignType.COUNTERSIGN -> {
            it.status = CountersigningStatus.AWAITING_COUNTERSIGN
          }
        }

        assertThat(it.updatedBy?.username).isEqualTo(userDetails.name)
      }

      planVersionRepository.getVersionByUuidAndVersion(planUuid, 1).let {
        assertThat(it.status).isEqualTo(CountersigningStatus.UNSIGNED)
        assertThat(it.updatedBy?.username).isEqualTo(userDetails.name)
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
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).startsWith("No resource found failure")
          assertThat(responseBody?.developerMessage).startsWith("No static resource")
        }
    }

    @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should return 409 conflict`() {
      val signRequest = SignRequest(
        signType = SignType.SELF,
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/sign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().is4xxClientError
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.CONFLICT.value())
          assertThat(responseBody?.developerMessage).isEqualTo("Plan $planUuid is in a DRAFT state, and not eligible for signing.")
        }
    }
  }

  @Nested
  @DisplayName("lockPlan")
  inner class LockPlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    val userDetails = UserDetails("1", "Tom C")

    @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should lock the plan and return a new version`() {
      val lockRequest = LockPlanRequest(
        userDetails = userDetails,
      )

      val beforePlanVersion = planRepository.getByUuid(planUuid).currentVersion
      val beforeVersionStatus = beforePlanVersion?.status!!
      val beforeVersion = beforePlanVersion.version

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/lock")
        .bodyValue(lockRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(beforeVersion.toLong())
        }

      val afterStatus = planVersionRepository.getVersionByUuidAndVersion(planUuid, beforeVersion).status
      val newPlanVersion = planRepository.getByUuid(planUuid).currentVersion

      assertThat(afterStatus).isNotEqualTo(beforeVersionStatus)
      assertThat(afterStatus).isEqualTo(CountersigningStatus.LOCKED_INCOMPLETE)
      assertThat(newPlanVersion?.version).isEqualTo(beforeVersion + 1)
      assertThat(newPlanVersion?.status).isEqualTo(CountersigningStatus.UNSIGNED)
    }

    @Test
    fun `should return 404 not found`() {
      val lockRequest = LockPlanRequest(
        userDetails = userDetails,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/lock")
        .bodyValue(lockRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan not found for id 0d0f2d85-5b70-4916-9f89-ed248f8d5196")
        }
    }
  }

  @Nested
  @DisplayName("Rollback Plan Version")
  inner class RollbackPlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val userDetails = UserDetails("1", "Tom C")

    @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should set the plan version to ROLLED_BACK`() {
      val beforePlanVersion = planRepository.getByUuid(planUuid).currentVersion
      val beforeVersionStatus = beforePlanVersion?.status!!
      val beforeVersion = beforePlanVersion.version

      val rollbackRequest = RollbackPlanRequest(
        userDetails = userDetails,
        sentencePlanVersion = beforeVersion.toLong(),
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/rollback")
        .bodyValue(rollbackRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(beforeVersion.toLong())
        }

      val afterStatus = planVersionRepository.getVersionByUuidAndVersion(planUuid, beforeVersion).status

      assertThat(afterStatus).isNotEqualTo(beforeVersionStatus)
      assertThat(afterStatus).isEqualTo(CountersigningStatus.ROLLED_BACK)
    }

    @Test
    fun `should return 404 not found`() {
      val rollbackRequest = RollbackPlanRequest(
        userDetails = userDetails,
        sentencePlanVersion = 999999L,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/rollback")
        .bodyValue(rollbackRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan version 999999 not found for Plan uuid 556db5c8-a1eb-4064-986b-0740d6a83c33")
        }
    }
  }

  @Nested
  @DisplayName("countersignPlan")
  inner class CountersignPlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    @Sql(
      scripts = ["/db/test/plan_data.sql", "/db/test/oasys_assessment_pk_data_awaiting_countersign.sql"],
      executionPhase = BEFORE_TEST_METHOD,
    )
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should countersign the plan and return the same version`() {
      val signRequest = CounterSignPlanRequest(
        signType = CountersignType.AWAITING_DOUBLE_COUNTERSIGN,
        sentencePlanVersion = 0L,
      )

      val beforePlanVersion = planRepository.getByUuid(planUuid).currentVersion
      val beforeVersionStatus = beforePlanVersion?.status!!
      val beforeVersion = beforePlanVersion.version

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/countersign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().run {
          assertThat(responseBody?.planId).isNotNull
          assertThat(responseBody?.planVersion).isEqualTo(beforeVersion.toLong())
        }

      val afterStatus = planVersionRepository.getVersionByUuidAndVersion(planUuid, beforeVersion).status

      assertThat(afterStatus).isNotEqualTo(beforeVersionStatus)
      assertThat(afterStatus).isEqualTo(CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN)
    }

    @Test
    fun `should return 404 not found`() {
      val signRequest = CounterSignPlanRequest(
        signType = CountersignType.AWAITING_DOUBLE_COUNTERSIGN,
        sentencePlanVersion = 0L,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/countersign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan version 0 not found for Plan uuid 0d0f2d85-5b70-4916-9f89-ed248f8d5196")
        }
    }

    @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should return 409 conflict`() {
      val signRequest = CounterSignPlanRequest(
        signType = CountersignType.REJECTED,
        sentencePlanVersion = 0L,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/countersign")
        .bodyValue(signRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().is4xxClientError
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.CONFLICT.value())
          assertThat(responseBody?.developerMessage).isEqualTo("Plan 556db5c8-a1eb-4064-986b-0740d6a83c33 was not awaiting countersign or double countersign.")
        }
    }
  }

  @Nested
  @DisplayName("softDeleteVersions")
  inner class SoftDeletePlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    @Sql(scripts = ["/db/test/oasys_assessment_pk_partial_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should successfully set all the remaining versions to soft deleted`() {
      val plan = planRepository.getByUuid(planUuid)
      val allVersionsBefore = planVersionRepository.findAllByPlanId(plan.id!!).filter { !it.softDeleted }
      assertThat(allVersionsBefore.size).isEqualTo(5)
      val softDeletePlanVersionsRequest = SoftDeletePlanVersionsRequest(
        userDetails = userDetails,
        from = 6,
      )
      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/soft-delete")
        .bodyValue(softDeletePlanVersionsRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<SoftDeletePlanVersionsResponse>()
        .returnResult().run {
          assertThat(responseBody?.versionsSoftDeleted).isEqualTo(listOf(6, 7, 8, 9))
          assertThat(responseBody?.planVersion).isEqualTo(5)
        }

      val planAfter = planRepository.getByUuid(planUuid)
      val allVersionsAfter = planVersionRepository.findAllByPlanId(plan.id!!).filter { !it.softDeleted }
      assertThat(allVersionsAfter.size).isEqualTo(1)
      assertThat(planAfter.currentVersion?.version).isEqualTo(5)
    }

    @Sql(scripts = ["/db/test/oasys_assessment_pk_no_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should return empty body if all records have been soft deleted`() {
      val plan = planRepository.getByUuid(planUuid)
      val allVersionsBefore = planVersionRepository.findAllByPlanId(plan.id!!).filter { !it.softDeleted }
      assertThat(allVersionsBefore.size).isEqualTo(10)
      val softDeletePlanVersionsRequest = SoftDeletePlanVersionsRequest(
        userDetails = userDetails,
        from = 0,
      )
      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/soft-delete")
        .bodyValue(softDeletePlanVersionsRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody()

      val planAfter = planRepository.getByUuid(planUuid)
      val allVersionsAfter = planVersionRepository.findAllByPlanId(plan.id!!).filter { !it.softDeleted }
      assertThat(allVersionsAfter.size).isEqualTo(0)
      assertThat(planAfter.currentVersion?.version).isEqualTo(9)
    }

    @Sql(scripts = ["/db/test/oasys_assessment_pk_partial_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should throw an error stating the supplied range contains versions already soft deleted`() {
      val softDeletePlanVersionsRequest = SoftDeletePlanVersionsRequest(
        userDetails = userDetails,
        from = 3,
      )
      val response = webTestClient.post()
        .uri("/coordinator/plan/$planUuid/soft-delete")
        .bodyValue(softDeletePlanVersionsRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult().responseBody

      assertThat(response?.userMessage).isEqualTo("Validation failure: The specified range contains version(s) (3, 4) that do not exist or have already had soft_deleted set to true")
    }

    @Test
    fun `should return 404 not found`() {
      val softDeletePlanVersionsRequest = SoftDeletePlanVersionsRequest(
        userDetails = userDetails,
        from = 3,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/soft-delete")
        .bodyValue(softDeletePlanVersionsRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan not found for id 0d0f2d85-5b70-4916-9f89-ed248f8d5196")
        }
    }
  }

  @Nested
  @DisplayName("restoreVersions")
  inner class RestorePlan {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    @Sql(scripts = ["/db/test/oasys_assessment_pk_partial_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should successfully restore the set of soft_deleted records`() {
      val plan = planRepository.getByUuid(planUuid)
      val allSoftDeletedVersionsBefore = planVersionRepository.findAllByPlanId(plan.id!!).filter { it.softDeleted }
      assertThat(allSoftDeletedVersionsBefore.size).isEqualTo(5)

      val restoreRequest = RestorePlanVersionsRequest(
        userDetails = userDetails,
        from = 0,
        to = 3,
      )
      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/restore")
        .bodyValue(restoreRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<SoftDeletePlanVersionsResponse>()
        .returnResult().run {
          assertThat(responseBody?.versionsRestored).isEqualTo(listOf(0, 1, 2))
          assertThat(responseBody?.planVersion).isEqualTo(9)
        }

      val allSoftDeletedVersionsAfter = planVersionRepository.findAllByPlanId(plan.id!!).filter { it.softDeleted }
      assertThat(allSoftDeletedVersionsAfter.size).isEqualTo(2)
    }

    @Sql(scripts = ["/db/test/oasys_assessment_pk_latest_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should successfully restore the latest soft_deleted record`() {
      val plan = planRepository.getByUuid(planUuid)
      val allSoftDeletedVersionsBefore = planVersionRepository.findAllByPlanId(plan.id!!).filter { it.softDeleted }
      assertThat(allSoftDeletedVersionsBefore.size).isEqualTo(1)

      val restoreRequest = RestorePlanVersionsRequest(
        userDetails = userDetails,
        from = 1,
        to = null,
      )
      webTestClient.post()
        .uri("/coordinator/plan/$planUuid/restore")
        .bodyValue(restoreRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<SoftDeletePlanVersionsResponse>()
        .returnResult().run {
          assertThat(responseBody?.versionsRestored).isEqualTo(listOf(1))
          assertThat(responseBody?.planVersion).isEqualTo(1)
        }

      val allSoftDeletedVersionsAfter = planVersionRepository.findAllByPlanId(plan.id!!).filter { it.softDeleted }
      assertThat(allSoftDeletedVersionsAfter.size).isEqualTo(0)

      val allNonDeletedVersionsAfter = planVersionRepository.findAllByPlanId(plan.id!!).filter { !it.softDeleted }
      assertThat(allNonDeletedVersionsAfter.size).isEqualTo(2)

      assertThat(planRepository.getByUuid(planUuid).currentVersion?.version).isEqualTo(1)
    }

    @Sql(scripts = ["/db/test/oasys_assessment_pk_partial_soft_deleted_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should throw an error stating the supplied range contains versions that are not soft deleted`() {
      val softDeletePlanVersionsRequest = SoftDeletePlanVersionsRequest(
        userDetails = userDetails,
        from = 0,
        to = 6,
      )
      val response = webTestClient.post()
        .uri("/coordinator/plan/$planUuid/restore")
        .bodyValue(softDeletePlanVersionsRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult().responseBody

      assertThat(response?.userMessage).isEqualTo("Validation failure: The specified range contains version(s) (5) that do not exist or have already had soft_deleted set to false")
    }

    @Test
    fun `should return 404 not found`() {
      val restoreRequest = RestorePlanVersionsRequest(
        userDetails = userDetails,
        from = 0,
        to = 4,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/restore")
        .bodyValue(restoreRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().run {
          assertThat(responseBody?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(responseBody?.userMessage).isEqualTo("Plan not found for id 0d0f2d85-5b70-4916-9f89-ed248f8d5196")
        }
    }
  }

  @Nested
  @DisplayName("Clone version")
  inner class ClonePlanVersion {
    val planUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
    val notFoundUuid = UUID.fromString("0d0f2d85-5b70-4916-9f89-ed248f8d5196")

    @Sql(scripts = ["/db/test/plan_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = ["/db/test/plan_cleanup.sql"], executionPhase = AFTER_TEST_METHOD)
    @Test
    fun `should clone the latest version into a new version`() {
      val beforePlan = planRepository.getByUuid(planUuid)
      assertThat(beforePlan.currentVersion?.version).isEqualTo(0L)

      val clonePlanVersionRequest = ClonePlanVersionRequest(
        planType = PlanType.OTHER,
        userDetails = userDetails,
      )

      val response = webTestClient.post()
        .uri("/coordinator/plan/$planUuid/clone")
        .bodyValue(clonePlanVersionRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionResponse>()
        .returnResult().responseBody

      val afterPlan = planRepository.getByUuid(planUuid)
      assertThat(afterPlan.currentVersion?.version).isEqualTo(1L)
      assertThat(afterPlan.currentVersion?.planType).isEqualTo(PlanType.OTHER)

      assertThat(response?.planVersion).isEqualTo(1L)
      assertThat(response?.planId).isEqualTo(planUuid)
    }

    @Test
    fun `should return 404 not found`() {
      val clonePlanVersionRequest = ClonePlanVersionRequest(
        userDetails = userDetails,
        planType = PlanType.OTHER,
      )

      webTestClient.post()
        .uri("/coordinator/plan/$notFoundUuid/clone")
        .bodyValue(clonePlanVersionRequest)
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
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
