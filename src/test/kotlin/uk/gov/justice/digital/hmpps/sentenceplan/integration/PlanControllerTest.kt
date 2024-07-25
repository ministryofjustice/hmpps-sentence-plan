package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.time.LocalDateTime
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Plan Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository
  lateinit var planUuid: UUID

  @BeforeAll
  fun setup() {
    val plan: PlanEntity = planRepository.findAll().first()
    planUuid = plan.uuid
  }

  @Nested
  @DisplayName("createPlan")
  inner class CreatePlan {
    @Test
    fun `should create a new plan`() {
      webTestClient.post().uri("/plans").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isCreated
        .expectBody<PlanEntity>()
    }
  }

  @Nested
  @DisplayName("getPlan")
  inner class GetPlan {
    @Test
    fun `should return OK when getting plan by existing UUID `() {
      webTestClient.get().uri("/plans/$planUuid")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `should return not found when getting plan by non-existent UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.get().uri("/plans/$randomPlanUuid")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("getPlanGoals")
  inner class GetPlanGoals {
    @Test
    fun `should return OK when getting goals by plan UUID`() {
      webTestClient.get().uri("/plans/$planUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList<GoalEntity>()
    }

    @Test
    fun `should return not found when getting goals by non-existent plan UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.get().uri("/plans/$randomPlanUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
        .expectBodyList<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("createNewGoal")
  inner class CreateNewGoal {
    private lateinit var goalRequestBody: Goal

    @BeforeEach
    fun setup() {
      goalRequestBody = Goal(
        title = "abc",
        areaOfNeed = "Accommodation",
        targetDate = LocalDateTime.now().toString(),
      )
    }

    @Test
    fun `should fail to create goal if Area of Need is not known`() {
      val goalRequestBodyBadAreaOfNeed = Goal(
        title = "abc",
        areaOfNeed = "doesn't exist",
        targetDate = LocalDateTime.now().toString(),
      )
      webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBodyBadAreaOfNeed)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `should fail to create goal if Plan does not exist`() {
      val goalRequestBodyBadAreaOfNeed = Goal(
        title = "abc",
        areaOfNeed = "doesn't exist",
        targetDate = LocalDateTime.now().toString(),
      )
      val randomUuid = UUID.randomUUID()
      val errorResponse: ErrorResponse? = webTestClient.post().uri("/plans/$randomUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBodyBadAreaOfNeed)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
        .returnResult().responseBody

      assertThat(errorResponse?.developerMessage).startsWith("A Plan with this UUID was not found:")
    }

    @Test
    fun `should create goal with no target date`() {
      val goalRequestBodyWithNoTargetDate = Goal(
        title = "abc",
        areaOfNeed = "ACCOMMODATION",
      )
      val goalEntity: GoalEntity? = webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBodyWithNoTargetDate)
        .exchange()
        .expectStatus().isCreated
        .expectBody<GoalEntity>()
        .returnResult().responseBody

      assertThat(goalEntity?.targetDate).isNull()
    }

    @Test
    fun `should create goal with Area of Need having a different case to DB field`() {
      val goalRequestBodyUppercaseAreaOfNeed = Goal(
        title = "abc",
        areaOfNeed = "ACCOMMODATION",
        targetDate = LocalDateTime.now().toString(),
      )
      val goalEntity: GoalEntity? = webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBodyUppercaseAreaOfNeed)
        .exchange()
        .expectStatus().isCreated
        .expectBody<GoalEntity>()
        .returnResult().responseBody

      val relatedAreasOfNeed = goalEntity?.relatedAreasOfNeed

      assertThat(relatedAreasOfNeed?.size).isZero()
    }

    @Test
    fun `should return created when creating goal with no related areas of need`() {
      val goalEntity: GoalEntity? = webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().isCreated
        .expectBody<GoalEntity>()
        .returnResult().responseBody

      val relatedAreasOfNeed: List<AreaOfNeedEntity>? = goalEntity?.relatedAreasOfNeed

      assertThat(relatedAreasOfNeed?.size).isZero()
    }

    @Test
    fun `should return created when creating goal with multiple related areas of need`() {
      goalRequestBody = Goal(
        title = "abc",
        areaOfNeed = "Accommodation",
        targetDate = LocalDateTime.now().toString(),
        relatedAreasOfNeed = listOf("Accommodation", "Finance"),
      )
      val goalEntity: GoalEntity? = webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().isCreated
        .expectBody<GoalEntity>()
        .returnResult().responseBody

      val relatedAreasOfNeed: List<AreaOfNeedEntity>? = goalEntity?.relatedAreasOfNeed

      assertThat(relatedAreasOfNeed?.size).isEqualTo(2)
    }

    @Test
    fun `should return server error when creating goal with invalid Plan UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.post().uri("/plans/$randomPlanUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }
}
