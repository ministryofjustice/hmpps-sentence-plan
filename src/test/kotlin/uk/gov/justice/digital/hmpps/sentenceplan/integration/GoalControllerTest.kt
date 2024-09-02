package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus
import java.time.LocalDateTime
import java.util.UUID

private const val TEST_DATA_GOAL_UUID = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Goal Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var planRepository: PlanRepository

  @Autowired
  lateinit var areaOfNeedRepository: AreaOfNeedRepository

  lateinit var goalRequestBody: Goal

  private val goalOrder = GoalOrder(
    goalUuid = UUID.randomUUID(),
    goalOrder = 1,
  )

  private val goalOrderList = listOf(goalOrder)

  private val stepOne = Step(
    description = "Step description",
    status = StepStatus.IN_PROGRESS,
    actor = "actor1",
  )

  private val stepTwo = Step(
    description = "Step description two",
    status = StepStatus.COMPLETED,
    actor = "actor2",
  )

  private val stepList: List<Step> = listOf(stepOne, stepTwo)

  private lateinit var plan: PlanEntity

  private var areaOfNeedName: String = ""

  @BeforeAll
  fun setup() {
    plan = planRepository.findAll().first()

    areaOfNeedName = areaOfNeedRepository.findAll().first().name

    goalRequestBody = Goal(
      title = "abc",
      areaOfNeed = areaOfNeedName,
      targetDate = LocalDateTime.now().toString(),
    )
  }

  @Nested
  @DisplayName("authTests")
  inner class GoalActionRoleTests {

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
    fun `get goal steps should return unauthorized when no auth token`() {
      webTestClient.get().uri("/plans/$plan.uuid/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isUnauthorized
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

  @Test
  fun `get goal by UUID should return OK when goal exists`() {
    val goal: GoalEntity? = webTestClient.get().uri("/goals/$TEST_DATA_GOAL_UUID")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isOk
      .expectBody<GoalEntity>()
      .returnResult().responseBody

    assertThat(goal?.areaOfNeed?.name).isEqualTo(areaOfNeedName)
    assertThat(goal?.areaOfNeed?.goals?.size).isNull()
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
  fun `get goal steps for UUID which doesn't exist should return not found`() {
    val randomUuid = UUID.randomUUID()
    webTestClient.get().uri("/goals/$randomUuid/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody<ErrorResponse>()
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
  fun `create goal steps with no steps should return 500`() {
    webTestClient.post().uri("/goals/${TEST_DATA_GOAL_UUID}/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(emptyList<StepEntity>())
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody<ErrorResponse>()
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
  fun `delete goal should return no content and confirm goal and steps deleted`() {
    webTestClient.delete().uri("/goals/ede47f7f-8431-4ff9-80ec-2dd3a8db3841")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNoContent

    webTestClient.get().uri("/goals/ede47f7f-8431-4ff9-80ec-2dd3a8db3841")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound

    webTestClient.get().uri("/steps/79803555-fad5-4cb7-8f8e-10f6d436834c")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `deleting a goal that does not exist should return 404`() {
    webTestClient.delete().uri("/goals/93ab5028-867f-4554-aa5a-2383e6b50f1f")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Nested
  @DisplayName("updateGoal")
  inner class UpdateGoalTests {

    @Test
    fun `should update goal title`() {
      val goalRequestBody = Goal(
        title = "New Goal Title",
        areaOfNeed = "Accommodation",
      )

      val goalUuid = "070442be-f855-4eb6-af7e-72f68aab54be"

      val goalEntity: GoalEntity? =
        webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo("New Goal Title")
    }

    @Test
    fun `should update and make a current goal`() {
      val goalRequestBody = Goal(
        title = "New Goal Title",
        areaOfNeed = "Accommodation",
        targetDate = "2024-06-25 10:00:00"
      )

      val goalUuid = "070442be-f855-4eb6-af7e-72f68aab54be"

      val goalEntity: GoalEntity? =
        webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo("New Goal Title")
      assertThat(goalEntity?.goalStatus).isEqualTo(GoalStatus.ACTIVE)
    }

    @Test
    fun `should update make a future goal`() {
      val goalRequestBody = Goal(
        title = "New Goal Title",
        areaOfNeed = "Accommodation",
        targetDate = null
      )

      val goalUuid = "070442be-f855-4eb6-af7e-72f68aab54be"

      val goalEntity: GoalEntity? =
        webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo("New Goal Title")
      assertThat(goalEntity?.goalStatus).isEqualTo(GoalStatus.FUTURE)
    }

    @Test
    fun `should update goal without changing area of need`() {
      val goalRequestBody = Goal(
        title = "Non Changing Area of Need Goal",
        areaOfNeed = "Finance",
      )

      val goalUuid = "070442be-f855-4eb6-af7e-72f68aab54be"

      val goalEntity: GoalEntity? =
        webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.areaOfNeed?.name).isEqualTo("Accommodation")
    }

    @Test
    fun `should update goal and delete related areas of need`() {
      val goalRequestBody = Goal(
        title = "Non Changing Area of Need Goal",
        areaOfNeed = "Finance",
      )

      val goalUuid = "070442be-f855-4eb6-af7e-72f68aab54be"

      val goalEntity: GoalEntity? =
        webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.relatedAreasOfNeed).isEmpty()
    }
  }

  @Nested
  @DisplayName("updateSteps")
  @Sql(scripts = [ "/db/test/update_steps_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/update_steps_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class UpdateStepsTests {

    @Test
    fun `update steps for goal with no steps should return list of new entities`() {
      val goalWithNoStepsUuid = "b9c66782-1dd0-4be5-910a-001e01313420"

      val steps: List<StepEntity>? = webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().isOk
        .expectBody<List<StepEntity>>()
        .returnResult().responseBody

      assertThat(steps?.size).isEqualTo(2)
      assertThat(steps!![0].actor).isEqualTo("actor1")
      assertThat(steps[1].actor).isEqualTo("actor2")
    }

    @Test
    fun `update steps for goal with existing step should return list of new entities`() {
      val goalWithOneStepUuid = "8b889730-ade8-4c3c-8e06-91a78b3ff3b2"

      val steps: List<StepEntity>? = webTestClient.put().uri("/goals/$goalWithOneStepUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().isOk
        .expectBody<List<StepEntity>>()
        .returnResult().responseBody

      assertThat(steps?.size).isEqualTo(2)
      assertThat(steps!![0].actor).isEqualTo("actor1")
      assertThat(steps[1].actor).isEqualTo("actor2")

      // refetch goal to make sure there are no surprise steps still attached
      val goal: GoalEntity? = webTestClient.get().uri("/goals/$goalWithOneStepUuid")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody<GoalEntity>()
        .returnResult().responseBody

      assertThat(goal?.steps?.size).isEqualTo(2)
      assertThat(goal?.steps!![0].actor).isEqualTo("actor1")
      assertThat(goal.steps[1].actor).isEqualTo("actor2")

      // now make sure the original Step no longer exists
      val originalGoalStepUuid = "fcf019dc-e9aa-44dd-ad9b-1f2f8ba06c99"

      webTestClient.get().uri("/steps/$originalGoalStepUuid")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for an unknown goal`() {
      val goalUuid = UUID.randomUUID()

      webTestClient.put().uri("/goals/$goalUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for a known goal when one step is incomplete`() {
      val goalWithNoStepsUuid = "b9c66782-1dd0-4be5-910a-001e01313420"

      val incompleteStep = Step(
        description = "Step description",
        status = StepStatus.NOT_STARTED,
        actor = "",
      )

      val listWithIncompleteStep: List<Step> = stepList + incompleteStep

      webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(listWithIncompleteStep)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for a known goal when list of steps is empty`() {
      val goalWithNoStepsUuid = "b9c66782-1dd0-4be5-910a-001e01313420"

      webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
        .bodyValue(emptyList<Step>())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }
}
