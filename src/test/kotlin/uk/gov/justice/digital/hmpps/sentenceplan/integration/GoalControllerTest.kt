package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import java.time.LocalDateTime
import java.util.UUID

private const val TEST_DATA_GOAL_UUID = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Goal Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  private var goalRequestBody: GoalEntity = GoalEntity(
    title = "abc",
    areaOfNeed = "xzv",
    targetDate = LocalDateTime.now().toString(),
    goalOrder = 1,
  )

  private val goalOrder = GoalOrder(
    goalUuid = UUID.randomUUID(),
    goalOrder = 1,
  )

  private val goalOrderList = listOf(goalOrder)

  private val stepOne = StepEntity(
    description = "Step description",
    actor = "Step actor",
    status = "incomplete",
  )

  private val stepTwo = StepEntity(
    description = "Step description two",
    actor = "Step actor two",
    status = "incomplete",
  )

  private val stepList: List<StepEntity> = listOf(stepOne, stepTwo)

  private lateinit var plan: PlanEntity

  @BeforeAll
  fun setup() {
    plan = planRepository.findAll().first()
  }

  @Test
  fun `create goal should return unauthorized when no auth token`() {
    webTestClient.post().uri("/plans/$plan.uuid/goals")
      .header("Content-Type", "application/json")
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create goal should return forbidden when no role`() {
    webTestClient.post().uri("/plans/$plan.uuid/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `create steps should return unauthorized when no auth token`() {
    webTestClient.post().uri("/plans/$plan.uuid/goals/1/steps")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create steps should return forbidden when no role`() {
    webTestClient.post().uri("/plans/$plan.uuid/goals/1/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goals should return forbidden when no role`() {
    webTestClient.get().uri("/plans/$plan.uuid/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goals should return unauthorized when no auth token`() {
    webTestClient.get().uri("/plans/$plan.uuid/goals")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get goal steps should return forbidden when no role`() {
    webTestClient.get().uri("/plans/$plan.uuid/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get goal by UUID should return OK when goal exists`() {
    webTestClient.get().uri("/goals/$TEST_DATA_GOAL_UUID")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
      .expectBody<GoalEntity>()
  }

  @Test
  fun `get goal by UUID should return NOT FOUND when goal does not exist`() {
    val randomUuid = UUID.randomUUID()
    webTestClient.get().uri("/goals/$randomUuid")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody<ErrorResponse>()
  }

  @Test
  fun `get goal steps should return OK and contain 1 step`() {
    webTestClient.get().uri("/goals/$TEST_DATA_GOAL_UUID/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList<StepEntity>()
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
  fun `create goal steps should return OK`() {
    webTestClient.post().uri("/goals/${TEST_DATA_GOAL_UUID}/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(stepList)
      .exchange()
      .expectStatus().isCreated
      .expectBodyList<StepEntity>().hasSize(2)
  }

  @Test
  fun `create goal steps should throw a DataIntegrityViolationException if the Goal GUID doesn't exist`() {
    val randomUuid = UUID.randomUUID()
    webTestClient.post().uri("/goals/$randomUuid/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(stepList)
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody<ErrorResponse>()
  }

  @Test
  fun `create goal steps with no steps should return CREATED`() {
    webTestClient.post().uri("/goals/${TEST_DATA_GOAL_UUID}/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(emptyList<StepEntity>())
      .exchange()
      .expectStatus().isCreated
      .expectBodyList<StepEntity>().hasSize(0)
  }

  @Test
  fun `get goal steps should return unauthorized when no auth token`() {
    webTestClient.get().uri("/plans/$plan.uuid/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
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
    webTestClient.post().uri("/plans/$plan.uuid/goals/order")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(goalOrderList)
      .exchange()
      .expectStatus().isForbidden
  }
}
