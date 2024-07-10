package uk.gov.justice.digital.hmpps.sentenceplan.integration

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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
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

  @Autowired
  lateinit var areaOfNeedRepository: AreaOfNeedRepository
  lateinit var areaOfNeedUuid: UUID

  @BeforeAll
  fun setup() {
    val plan: PlanEntity = planRepository.findAll().first()
    planUuid = plan.uuid

    val areaOfNeed: AreaOfNeedEntity = areaOfNeedRepository.findAll().first()
    areaOfNeedUuid = areaOfNeed.uuid
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
    fun `should return empty list when getting goals by non-existent plan UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.get().uri("/plans/$randomPlanUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList<GoalEntity>().hasSize(0)
    }
  }

  @Nested
  @DisplayName("createNewGoal")
  inner class CreateNewGoal {
    lateinit var goalRequestBody: GoalEntity

    @BeforeEach
    fun setup() {
      goalRequestBody = GoalEntity(
        title = "abc",
        areaOfNeedUuid = areaOfNeedUuid,
        targetDate = LocalDateTime.now().toString(),
        goalOrder = 1,
      )
    }

    @Test
    fun `should return created when creating goal`() {
      webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().isCreated
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
