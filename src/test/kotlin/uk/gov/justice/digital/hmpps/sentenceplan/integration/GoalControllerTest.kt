package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import java.time.LocalDateTime
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Goal Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  var goalRequestBody: GoalEntity? = null

  private val goalOrder = GoalOrder(
    goalUuid = UUID.randomUUID(),
    goalOrder = 1,
  )

  private val goalOrderList = listOf(goalOrder)

  val currentTime = LocalDateTime.now().toString()

  @BeforeAll
  fun setup() {
    val plan: PlanEntity = planRepository.findAll().first()

    goalRequestBody = GoalEntity(
      title = "abc",
      areaOfNeed = "xzv",
      creationDate = currentTime,
      targetDate = currentTime,
      goalOrder = 1,
      planUuid = plan.uuid,
    )
  }

  @Test
  fun `create goal should return created`() {
    webTestClient.post().uri("/goals").header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isCreated
  }

  @Test
  fun `create goal should return unauthorized when no auth token`() {
    webTestClient.post().uri("/goals")
      .header("Content-Type", "application/json")
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create goal should return forbidden when no role`() {
    webTestClient.post().uri("/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `create steps should return unauthorized when no auth token`() {
    webTestClient.post().uri("/goals/1/steps")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create steps should return forbidden when no role`() {
    webTestClient.post().uri("/goals/1/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goals should return forbidden when no role`() {
    webTestClient.get().uri("/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goals should return OK`() {
    webTestClient.get().uri("/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get goals should return unauthorized when no auth token`() {
    webTestClient.get().uri("/goals")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get goal steps should return forbidden when no role`() {
    webTestClient.get().uri("/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goal steps should return OK`() {
    webTestClient.get().uri("/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get goal steps for UUID which doesn't exist should return OK and an empty list`() {
    val randomUuid = UUID.randomUUID()
    webTestClient.get().uri("/goals/$randomUuid/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectBodyList<StepEntity>().hasSize(0)
  }

  @Test
  fun `get goal steps should return unauthorized when no auth token`() {
    webTestClient.get().uri("/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `update goals order should return created`() {
    webTestClient.post().uri("/goals/order")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(goalOrderList)
      .exchange()
      .expectStatus().isCreated
  }

  @Test
  fun `update goals order should return unauthorized when no auth token`() {
    webTestClient.post().uri("/goals/order")
      .header("Content-Type", "application/json")
      .bodyValue(goalOrderList)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `update goals order should return forbidden when no role`() {
    webTestClient.post().uri("/goals/order")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(goalOrderList)
      .exchange()
      .expectStatus().isForbidden
  }
}
