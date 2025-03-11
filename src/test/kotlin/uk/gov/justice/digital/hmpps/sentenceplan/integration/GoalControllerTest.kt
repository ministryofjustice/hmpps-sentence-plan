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
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalStatusUpdate
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val TEST_DATA_GOAL_UUID = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Goal Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var goalRepository: GoalRepository

  lateinit var goalRequestBody: Goal

  val authenticatedUser = UUID.randomUUID().toString() + "|Tom C"

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

  private var areaOfNeedName: String = "Accommodation"

  @BeforeAll
  fun setup() {
    goalRequestBody = Goal(
      title = "abc",
      areaOfNeed = areaOfNeedName,
      targetDate = LocalDate.now().toString(),
    )
  }

  @Nested
  @DisplayName("authTests")
  inner class GoalActionRoleTests {

    val randomUuid = UUID.randomUUID()

    @Test
    fun `create goal should return unauthorized when no auth token`() {
      webTestClient.post().uri("/plans/$randomUuid/goals")
        .header("Content-Type", "application/json")
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `create goal should return forbidden when no role`() {
      webTestClient.post().uri("/plans/$randomUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(roles = listOf("abc")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create steps should return unauthorized when no auth token`() {
      webTestClient.post().uri("/plans/$randomUuid/goals/1/steps")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `create steps should return forbidden when no role`() {
      webTestClient.post().uri("/plans/$randomUuid/goals/1/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(roles = listOf("abc")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get goals should return forbidden when no role`() {
      webTestClient.get().uri("/plans/$randomUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(roles = listOf("abc")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get goals should return unauthorized when no auth token`() {
      webTestClient.get().uri("/plans/$randomUuid/goals")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get goal steps should return forbidden when no role`() {
      webTestClient.get().uri("/plans/$randomUuid/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(roles = listOf("abc")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get goal steps should return unauthorized when no auth token`() {
      webTestClient.get().uri("/plans/$randomUuid/goals/e6fb513d-3800-4c35-bb3a-5f9bdc9759dd/steps")
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
      webTestClient.post().uri("/plans/$randomUuid/goals/order")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(roles = listOf("abc")))
        .bodyValue(goalOrderList)
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  @DisplayName("getterTests")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GoalControllerGetterTests {
    @Test
    fun `get goal by UUID should return OK when goal exists`() {
      val goal: GoalEntity? = webTestClient.get().uri("/goals/$TEST_DATA_GOAL_UUID")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
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
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `get goal steps should return OK and contain 1 step`() {
      webTestClient.get().uri("/goals/$TEST_DATA_GOAL_UUID/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList<StepEntity>()
    }

    @Test
    fun `get goal steps for UUID which doesn't exist should return not found`() {
      val randomUuid = UUID.randomUUID()
      webTestClient.get().uri("/goals/$randomUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("createTests")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GoalControllerCreatorTests {
    @Test
    fun `create goal steps should return OK`() {
      webTestClient.post().uri("/goals/${TEST_DATA_GOAL_UUID}/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().isCreated
        .expectBodyList<StepEntity>().hasSize(2)
    }

    @Test
    fun `create goal steps should throw a 404 if the Goal GUID doesn't exist`() {
      val randomUuid = UUID.randomUUID()
      webTestClient.post().uri("/goals/$randomUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `create goal steps with no steps should return 500`() {
      webTestClient.post().uri("/goals/${TEST_DATA_GOAL_UUID}/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(emptyList<StepEntity>())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("updateGoalOrderTests")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GoalControllerUpdateGoalOrderTests {
    @Test
    fun `update goals order should return created`() {
      webTestClient.post().uri("/goals/order")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(goalOrderList)
        .exchange()
        .expectStatus().isCreated
    }
  }

  @Nested
  @DisplayName("deleteGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GoalControllerDeleteTests {

    @Test
    @Sql(scripts = ["/db/test/goal_deletion_data.sql"], executionPhase = BEFORE_TEST_METHOD)
    fun `delete goal should return no content and confirm goal and steps deleted`() {
      webTestClient.delete().uri("/goals/ede47f7f-8431-4ff9-80ec-2dd3a8db3841")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/goals/ede47f7f-8431-4ff9-80ec-2dd3a8db3841")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.get().uri("/steps/79803555-fad5-4cb7-8f8e-10f6d436834c")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `deleting a goal that does not exist should return 404`() {
      webTestClient.delete().uri("/goals/93ab5028-867f-4554-aa5a-2383e6b50f1f")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("replaceGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  inner class ReplaceGoalTests {

    @Test
    fun `should update goal title`() {
      val goalRequestBody = Goal(
        title = "Update Goal Title Test",
        areaOfNeed = "Accommodation",
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo("Update Goal Title Test")
    }

    @Test
    fun `should update and make a current goal`() {
      val goalRequestBody = Goal(
        title = "Update goal target date test",
        areaOfNeed = "Accommodation",
        targetDate = "2024-06-25",
      )

      val goalUuid = "778b8e52-5927-42d4-9c05-7029ef3c6f6d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo(goalRequestBody.title)
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.ACTIVE)
    }

    @Test
    fun `should update a future goal`() {
      val goalRequestBody = Goal(
        title = "Update a future goal test",
        areaOfNeed = "Accommodation",
        targetDate = null,
        status = GoalStatus.FUTURE,
      )

      val goalUuid = "778b8e52-5927-42d4-9c05-7029ef3c6f6d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo(goalRequestBody.title)
      assertThat(goalEntity?.targetDate).isNull()
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.FUTURE)
    }

    @Test
    fun `should update an active goal into a future goal`() {
      val goalRequestBody = Goal(
        targetDate = null,
        title = "Change active goal into future goal test",
        areaOfNeed = "Accommodation",
        status = GoalStatus.FUTURE,
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.title).isEqualTo(goalRequestBody.title)
      assertThat(goalEntity?.targetDate).isNull()
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.FUTURE)
    }

    @Test
    fun `should update goal without changing area of need`() {
      val goalRequestBody = Goal(
        title = "Update goal without changing area of need test",
        areaOfNeed = "Finances",
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.areaOfNeed?.name).isEqualTo("Accommodation")
    }

    @Test
    fun `should add a note to a goal without updating status`() {
      val goalRequestBody = Goal(
        title = "Goal For Now Title",
        areaOfNeed = "Accommodation",
        note = "An exciting note",
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.notes?.size).isEqualTo(1)
      assertThat(goalEntity?.notes?.first()?.note).isEqualTo("An exciting note")
      assertThat(goalEntity?.notes?.first()?.type).isEqualTo(GoalNoteType.PROGRESS)
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.ACTIVE)
    }

    @Test
    fun `should add a note to a goal and update status to ACHIEVED`() {
      val goalRequestBody = Goal(
        title = "A title",
        note = "An exciting note",
        areaOfNeed = "Accommodation",
        status = GoalStatus.ACHIEVED,
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.notes?.size).isEqualTo(1)
      assertThat(goalEntity?.notes?.first()?.note).isEqualTo("An exciting note")
      assertThat(goalEntity?.notes?.first()?.type).isEqualTo(GoalNoteType.ACHIEVED)
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.ACHIEVED)
    }

    @Test
    fun `should add a note to a goal and update status to REMOVED`() {
      val goalRequestBody = Goal(
        title = "A title",
        note = "An exciting note",
        areaOfNeed = "Finances",
        status = GoalStatus.REMOVED,
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.notes?.size).isEqualTo(1)
      assertThat(goalEntity?.notes?.first()?.note).isEqualTo("An exciting note")
      assertThat(goalEntity?.notes?.first()?.type).isEqualTo(GoalNoteType.REMOVED)
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.REMOVED)
    }

    @Test
    @Sql(scripts = [ "/db/test/related_area_of_need_deletion_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
    @SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
    fun `should update goal and delete related areas of need`() {
      val goalRequestBody = Goal(
        title = "update goal and delete related areas of need",
        areaOfNeed = "Finances",
      )

      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val goalEntity: GoalEntity? =
        webTestClient.put().uri("/goals/$goalUuid").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isOk
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.relatedAreasOfNeed).isEmpty()
    }
  }

  @Nested
  @DisplayName("achieveGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/related_area_of_need_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/related_area_of_need_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  open inner class AchieveGoalTests {
    @Test
    @Transactional
    open fun `achieve goal without a note`() {
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"
      val note = Goal(note = "")

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/achieve").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(note)
        .exchange()
        .expectStatus().isOk

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.ACHIEVED)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.first().note).isEqualTo("")
      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.ACHIEVED)
    }

    @Test
    @Transactional
    open fun `achieve goal with a note and related areas of need are preserved`() {
      val note = Goal(note = "A note")
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/achieve").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(note)
        .exchange()
        .expectStatus().isOk

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.ACHIEVED)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.first().note).isEqualTo("A note")
      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.ACHIEVED)
    }
  }

  @Nested
  @DisplayName("removeGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/related_area_of_need_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/related_area_of_need_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  open inner class RemoveGoalTests {
    @Test
    @Transactional
    open fun `remove goal with a note`() {
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"
      val note = Goal(note = "Removing note")

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/remove").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(note)
        .exchange()
        .expectStatus().isOk

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.REMOVED)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.first().note).isEqualTo("Removing note")
      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.REMOVED)
    }

    @Test
    @Transactional
    open fun `remove goal without a note should fail`() {
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"
      val note = Goal(note = "")

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/remove").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(note)
        .exchange()
        .expectStatus().is4xxClientError

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.ACTIVE)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.size).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("reAddGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/related_area_of_need_data.sql", "/db/test/goal_removed.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/related_area_of_need_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  open inner class ReaddGoalTests {

    @Test
    open fun `re-add goal with a note and no target date`() {
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"
      val note = Goal(note = "Re-adding note")

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/readd").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(note)
        .exchange()
        .expectStatus().isOk

//      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
//      assertThat(goal.status).isEqualTo(GoalStatus.FUTURE)
//      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
//      assertThat(goal.notes.first().note).isEqualTo("Re-adding note")
//      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.READDED)
    }

    //    @Sql(scripts = ["/db/test/goal_removed.sql"], executionPhase = BEFORE_TEST_METHOD)
    @Test
    fun `re-add goal with a note and a target date`() {
      val goalUuid = "778b8e52-5927-42d4-9c05-7029ef3c6f6d" // goal for the future
      val reAddGoal = Goal(note = "Re-adding note", targetDate = LocalDate.now().plusWeeks(20).format(DateTimeFormatter.ISO_LOCAL_DATE))

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.post().uri("/goals/$goalUuid/readd").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(reAddGoal)
        .exchange()
        .expectStatus().isOk
        .expectBody<GoalEntity>()
        .consumeWith { response ->
          val goal = response.responseBody
          assertThat(goal?.status).isEqualTo(GoalStatus.ACTIVE)
          assertThat(goal?.targetDate).isEqualTo(LocalDate.now().plusWeeks(20))
          assertThat(goal?.relatedAreasOfNeed).isEmpty()
          assertThat(goal?.notes?.first()?.note).isEqualTo("Re-adding note")
          assertThat(goal?.notes?.first()?.type).isEqualTo(GoalNoteType.READDED)
        }
    }

    /** currently unsupported in GoalService
     @Test
     @Transactional
     open fun `re-add goal without a note should fail`() {
     val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"
     val note = Goal(note = "")

     // If you goalRepository.findByUuid() here, the test will fail.

     webTestClient.post().uri("/goals/$goalUuid/readd").header("Content-Type", "application/json")
     .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
     .bodyValue(note)
     .exchange()
     .expectStatus().is4xxClientError

     val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
     assertThat(goal.status).isEqualTo(GoalStatus.ACTIVE)
     assertThat(goal.relatedAreasOfNeed).isNotEmpty()
     assertThat(goal.notes.size).isEqualTo(0)
     }*/
  }

  @Nested
  @DisplayName("updateGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/related_area_of_need_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/related_area_of_need_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  open inner class UpdateGoalTests {

    @Test
    @Transactional
    open fun `update goal status without a note`() {
      val goalStatusUpdate = GoalStatusUpdate(
        status = GoalStatus.FUTURE,
        note = "",
      )
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(goalStatusUpdate)
        .exchange()
        .expectStatus().isOk

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.FUTURE)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.first().note).isEqualTo("")
      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.PROGRESS)
    }

    @Test
    @Transactional
    open fun `update goal status with a note and related areas of need are preserved`() {
      val goalStatusUpdate = GoalStatusUpdate(
        status = GoalStatus.FUTURE,
        note = "A note",
      )
      val goalUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      // If you goalRepository.findByUuid() here, the test will fail.

      webTestClient.patch().uri("/goals/$goalUuid").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(goalStatusUpdate)
        .exchange()
        .expectStatus().isOk

      val goal = goalRepository.getGoalByUuid(UUID.fromString(goalUuid))
      assertThat(goal.status).isEqualTo(GoalStatus.FUTURE)
      assertThat(goal.relatedAreasOfNeed).isNotEmpty()
      assertThat(goal.notes.first().note).isEqualTo("A note")
      assertThat(goal.notes.first().type).isEqualTo(GoalNoteType.PROGRESS)
    }
  }

  @Nested
  @DisplayName("updateSteps")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class UpdateStepsTests {

    @Test
    fun `update steps for goal with no steps should return list of new entities`() {
      val goalWithNoStepsUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val steps: List<StepEntity>? = webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(Goal(steps = stepList))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<StepEntity>>()
        .returnResult().responseBody

      assertThat(steps?.size).isEqualTo(2)
      assertThat(steps!![0].actor).isEqualTo("actor1")
      assertThat(steps[1].actor).isEqualTo("actor2")
    }

    @Test
    @Sql(scripts = [ "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = [ "/db/test/step_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
    fun `update steps for goal with existing step should return list of new entities`() {
      val goalWithOneStepUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val steps: List<StepEntity>? = webTestClient.put().uri("/goals/$goalWithOneStepUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(Goal(steps = stepList))
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
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
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
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for an unknown goal`() {
      val goalUuid = UUID.randomUUID()

      webTestClient.put().uri("/goals/$goalUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(stepList)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for a known goal when one step is incomplete`() {
      val goalWithNoStepsUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      val incompleteStep = Step(
        description = "Step description",
        status = StepStatus.NOT_STARTED,
        actor = "",
      )

      val listWithIncompleteStep: List<Step> = stepList + incompleteStep

      webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(listWithIncompleteStep)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }

    @Test
    fun `update steps should fail for a known goal when list of steps is empty`() {
      val goalWithNoStepsUuid = "31d7e986-4078-4f5c-af1d-115f9ba3722d"

      webTestClient.put().uri("/goals/$goalWithNoStepsUuid/steps")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(emptyList<Step>())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }
}
