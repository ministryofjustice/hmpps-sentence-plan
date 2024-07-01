package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.time.LocalDateTime
import java.util.*

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Plan Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  var planUuid: UUID? = null
  var goalRequestBody: GoalEntity? = null

  @BeforeAll
  fun setup() {
    val plan: PlanEntity = planRepository.findAll().first()
    planUuid = plan.uuid

    goalRequestBody = GoalEntity(
      title = "abc",
      areaOfNeed = "xzv",
      targetDate = LocalDateTime.now().toString(),
      goalOrder = 1,
    )
  }

  @Test
  fun `get plan by existing UUID should return OK`() {
    webTestClient.get().uri("/plans/$planUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get plan by non-existent UUID should return not found`() {
    val randomPlanUuid = UUID.randomUUID()
    webTestClient.get().uri("/plans/$randomPlanUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `get goals by plan UUID should return OK`() {
    webTestClient.get().uri("/plans/$planUuid/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `create goal should return created`() {
    webTestClient.post().uri("/plans/$planUuid/goals").header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isCreated
  }
}
