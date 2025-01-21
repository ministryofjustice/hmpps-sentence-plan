package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Note
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@AutoConfigureWebTestClient(timeout = "5s")
@DisplayName("Plan Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanControllerTest : IntegrationTestBase() {

  val authenticatedUser = UUID.randomUUID().toString() + "|Tom C"
  val testPlanUuid = "556db5c8-a1eb-4064-986b-0740d6a83c33"

  @Nested
  @DisplayName("getPlan")
//  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
//  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlan {
    @Test
    fun `should return OK when getting plan by existing UUID `() {
      val planVersionEntity: PlanVersionEntity? = webTestClient.get().uri("/plans/$testPlanUuid")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PlanVersionEntity>()
        .returnResult().responseBody

      assertThat(planVersionEntity?.goals?.size).isEqualTo(2)
      assertThat(planVersionEntity?.mostRecentUpdateDate).isNotNull()
      assertThat(planVersionEntity?.mostRecentUpdateDate).isAfter(planVersionEntity?.updatedDate)
      assertThat(planVersionEntity?.mostRecentUpdateByName).isNotNull()
      assertThat(planVersionEntity?.mostRecentUpdateByName).isEqualTo("test user")
    }

    @Test
    fun `should return not found when getting plan by non-existent UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.get().uri("/plans/$randomPlanUuid")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("getPlanGoals")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlanGoals {
    @Test
    fun `should return OK when getting goals by plan UUID`() {
      val goalsMap: Map<String, List<GoalEntity>>? =
        webTestClient.get().uri("/plans/$testPlanUuid/goals")
          .header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
          .exchange()
          .expectStatus().isOk
          .expectBody<Map<String, List<GoalEntity>>>()
          .returnResult().responseBody

      assertThat(goalsMap).isNotNull
      assertThat(goalsMap?.size).isEqualTo(2)
      assertThat(goalsMap?.get("now")?.first()?.title).isEqualTo("Goal For Now Title")
      assertThat(goalsMap?.get("future")?.first()?.title).isEqualTo("Goal For Future Title")
    }

    // TODO  make sure there is a test where goal.updatedDate is later than plan.lastUpdatedDate (ideally harmonise these names!)
    // and then check the value of mostRecentUpdateDate

    @Test
    fun `should return not found when getting goals by non-existent plan UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.get().uri("/plans/$randomPlanUuid/goals")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ")))
        .exchange()
        .expectStatus().isNotFound
        .expectBodyList<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("createNewGoal")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class CreateNewGoal {
    private lateinit var goalRequestBody: Goal

    @BeforeEach
    fun setup() {
      goalRequestBody = Goal(
        title = "CreateNewGoal Test data",
        areaOfNeed = "Accommodation",
        targetDate = LocalDate.now().toString(),
      )
    }

    @Test
    fun `should fail to create goal if Area of Need is not known`() {
      val goalRequestBodyBadAreaOfNeed = Goal(
        title = "abc",
        areaOfNeed = "doesn't exist",
        targetDate = LocalDate.now().toString(),
      )
      webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
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
        targetDate = LocalDate.now().toString(),
      )
      val randomUuid = UUID.randomUUID()
      val errorResponse: ErrorResponse? = webTestClient.post().uri("/plans/$randomUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
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
      val goalEntity: GoalEntity? =
        webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBodyWithNoTargetDate)
          .exchange()
          .expectStatus().isCreated
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.targetDate).isNull()
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.FUTURE)
    }

    @Test
    fun `should create goal with a target date`() {
      val goalEntity: GoalEntity? =
        webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isCreated
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.targetDate).isNotNull()
      assertThat(goalEntity?.status).isEqualTo(GoalStatus.ACTIVE)
    }

    @Test
    fun `should create goal with Area of Need having a different case to DB field`() {
      val goalRequestBodyUppercaseAreaOfNeed = Goal(
        title = "abc",
        areaOfNeed = "ACCOMMODATION",
        targetDate = LocalDate.now().toString(),
      )
      val goalEntity: GoalEntity? =
        webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBodyUppercaseAreaOfNeed)
          .exchange()
          .expectStatus().isCreated
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      assertThat(goalEntity?.relatedAreasOfNeed?.size).isZero()
    }

    @Test
    fun `should return created when creating goal with no related areas of need`() {
      val goalEntity: GoalEntity? =
        webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isCreated
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      val relatedAreasOfNeed: Set<AreaOfNeedEntity>? = goalEntity?.relatedAreasOfNeed

      assertThat(relatedAreasOfNeed?.size).isZero()
    }

    @Test
    @Sql(scripts = [ "/db/test/related_area_of_need_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
    fun `should return created when creating goal with multiple related areas of need`() {
      goalRequestBody = Goal(
        title = "abc",
        areaOfNeed = "Accommodation",
        targetDate = LocalDate.now().toString(),
        relatedAreasOfNeed = listOf("Accommodation", "Finances"),
      )
      val goalEntity: GoalEntity? =
        webTestClient.post().uri("/plans/$testPlanUuid/goals").header("Content-Type", "application/json")
          .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
          .bodyValue(goalRequestBody)
          .exchange()
          .expectStatus().isCreated
          .expectBody<GoalEntity>()
          .returnResult().responseBody

      val relatedAreasOfNeed: Set<AreaOfNeedEntity>? = goalEntity?.relatedAreasOfNeed

      assertThat(relatedAreasOfNeed?.size).isEqualTo(2)
    }

    @Test
    fun `should return server error when creating goal with invalid Plan UUID`() {
      val randomPlanUuid = UUID.randomUUID()
      webTestClient.post().uri("/plans/$randomPlanUuid/goals").header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(goalRequestBody)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody<ErrorResponse>()
    }
  }

  @Nested
  @DisplayName("agreePlan")
  @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
  @Sql(scripts = [ "/db/test/plan_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class AgreePlan {
    private val agreePlanBody = Agreement(
      PlanAgreementStatus.AGREED,
      "Agreed",
      "Note",
      "Sarah B",
      "Tom C",
    )

    @Test
    @Order(1)
    fun `agree plan`() {
      val testStartTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

      val planVersionEntity: PlanVersionEntity? = webTestClient.post().uri("/plans/$testPlanUuid/agree")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(agreePlanBody)
        .exchange()
        .expectStatus().isAccepted
        .expectBody<PlanVersionEntity>()
        .returnResult().responseBody

      assertThat(planVersionEntity?.agreementDate).isNotNull()
      assertThat(planVersionEntity?.updatedBy?.username).isEqualTo("Tom C")
      assertThat(planVersionEntity?.updatedDate).isAfterOrEqualTo(testStartTime)
      assertThat(planVersionEntity?.agreementDate).isBeforeOrEqualTo(planVersionEntity?.updatedDate)
    }

    @Test
    @Order(2)
    fun `plan has already been agreed`() {
      webTestClient.post().uri("/plans/$testPlanUuid/agree")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(agreePlanBody)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `plan not found`() {
      webTestClient.post().uri("/plans/e0b7707e-a9da-4574-b97f-ea84e402baf6/agree")
        .header("Content-Type", "application/json")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .bodyValue(agreePlanBody)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    }
  }

  @Nested
  @DisplayName("get plan and goal notes")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/plan_notes_data.sql", "/db/test/goals_data.sql", "/db/test/goal_notes_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/goal_notes_cleanup.sql", "/db/test/plan_notes_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
  inner class GetPlanAndGoalNotes {
    @Test
    fun `should fetch all plan and goal notes for a given plan uuid`() {
      val notes: List<Note>? = webTestClient.get().uri("/plans/$testPlanUuid/notes")
        .headers(setAuthorisation(user = authenticatedUser, roles = listOf("ROLE_SENTENCE_PLAN_READ", "ROLE_SENTENCE_PLAN_WRITE")))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<Note>>()
        .returnResult().responseBody

      assertThat(notes!!).isNotNull

      assertThat(notes.size).isEqualTo(3)
      assertThat(notes[0].createdDate).isAfterOrEqualTo(notes[1].createdDate)

      // make sure all the values are populated correctly for a Goal(checks the ResultMapper)
      assertThat(notes[0].noteObject).isEqualTo("Goal")
      assertThat(notes[0].note).isEqualTo("Second goal note")
      assertThat(notes[0].additionalNote).isNull()
      assertThat(notes[0].noteType).isEqualTo("ACHIEVED")
      assertThat(notes[0].goalTitle).isEqualTo("Goal For Now Title")
      assertThat(notes[0].goalUuid).isEqualTo("31d7e986-4078-4f5c-af1d-115f9ba3722d")
      assertThat(notes[0].createdBy).isEqualTo("test user")

      // make sure all the values are populated correctly for a Plan(checks the ResultMapper)
      assertThat(notes[2].noteObject).isEqualTo("Plan")
      assertThat(notes[2].note).isEqualTo("Agreement status note")
      assertThat(notes[2].additionalNote).isEqualTo("Optional note")
      assertThat(notes[2].noteType).isEqualTo("AGREED")
      assertThat(notes[2].goalTitle).isNull()
      assertThat(notes[2].goalUuid).isNull()
      assertThat(notes[2].createdBy).isEqualTo("test user")
    }
  }
}
