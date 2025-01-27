package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.CountersigningStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanProgressNotesRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import uk.gov.justice.digital.hmpps.sentenceplan.services.VersionService
import java.util.UUID

@DisplayName("Version Service Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class VersionServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var planProgressNoteRepository: PlanProgressNotesRepository

  @Autowired
  lateinit var versionService: VersionService

  @Autowired
  lateinit var planRepository: PlanRepository

  @Autowired
  lateinit var planVersionRepository: PlanVersionRepository

  @Autowired
  lateinit var goalRepository: GoalRepository

  @Autowired
  lateinit var goalService: GoalService

  @Autowired
  lateinit var planService: PlanService

  // from SQL scripts
  val testPlanUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")!!
  val testPlanVersionUuid = UUID.fromString("9f2aaa46-e544-4bcd-8db6-fbe7842ddb64")!!
  var newPlanVersionUuid: UUID = UUID.randomUUID()

  @Test
  @DisplayName("Create new PlanVersion")
  @WithMockUser(username = "UserId|Username")
  @Sql(
    scripts = [
      "/db/test/plan_data.sql",
      "/db/test/goals_data.sql",
      "/db/test/related_area_of_need_data.sql",
      "/db/test/step_data.sql",
      "/db/test/plan_notes_data.sql",
    ],
    executionPhase = BEFORE_TEST_METHOD,
  )
  @Sql(
    scripts = [
      "/db/test/plan_notes_cleanup.sql",
      "/db/test/step_cleanup.sql",
      "/db/test/related_area_of_need_cleanup.sql",
      "/db/test/goals_cleanup.sql",
      "/db/test/plan_cleanup.sql",
    ],
    executionPhase = AFTER_TEST_METHOD,
  )
  fun `test new plan version has the same associations as previous version`() {
    // a plan and a version are added via SQL scripts in annotation

    // create a new version
    val planVersion = planVersionRepository.findByUuid(testPlanVersionUuid)
    val planVersionOne = versionService.alwaysCreateNewPlanVersion(planVersion)
    newPlanVersionUuid = planVersionOne.uuid

    // now check there are two versions
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    // check that when fetched the plan references the version with ID 1
    val planEntity = planRepository.findByUuid(testPlanUuid)
    assertThat(planEntity.currentVersion!!.version).isEqualTo(1)

    // check that the UUID of version 0 is now different to the original UUID
    val planVersionZero = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0)
    assertThat(planVersionZero.uuid).isNotEqualTo(testPlanVersionUuid)

    // check that both versions reference the same plan
    assertThat(planVersionZero.planId).isEqualTo(planVersionOne.planId)

    // check that the latest version references the plan and that the previous version is null (reference is owned by PlanEntity)
    assertThat(planVersionOne.plan!!).isNotNull
    assertThat(planVersionZero.plan).isNull()

    // check that each version has two goals
    assertThat(planVersionZero.goals.size).isEqualTo(planVersionOne.goals.size)
    assertThat(goalRepository.findAll()).hasSize(4)

    // check that the first step in the first goal of each plan version have matching descriptions but different UUIDs
    val planVersionZeroFirstGoal = goalRepository.findByUuidWithSteps(planVersionZero.goals.toList()[0].uuid)
    val planVersionOneFirstGoal = goalRepository.findByUuidWithSteps(planVersionOne.goals.toList()[0].uuid)

    assertThat(planVersionZeroFirstGoal.steps[0].description).isEqualTo(planVersionZeroFirstGoal.steps[0].description)
    assertThat(planVersionZeroFirstGoal.steps[0].uuid).isNotEqualTo(planVersionOneFirstGoal.steps[0].uuid)

    // check that each planVersion has its own PlanAgreementNote
    assertThat(planVersionZero.agreementNote).isNotNull
    assertThat(planVersionOne.agreementNote).isNotNull
    assertThat(planVersionZero.agreementNote?.id).isNotEqualTo(planVersionOne.agreementNote?.id)

    // fetch progress notes by plan version UUID and check that each planVersion has its own PlanProgressNotes
    val planVersionZeroProgressNotes = planProgressNoteRepository.findByPlanVersionUuid(planVersionZero.uuid)
    assertThat(planVersionZeroProgressNotes.size).isGreaterThan(0)

    val planVersionOneProgressNotes = planProgressNoteRepository.findByPlanVersionUuid(planVersionOne.uuid)
    assertThat(planVersionOneProgressNotes.size).isGreaterThan(0)

    assertThat(planVersionZeroProgressNotes[0].id).isNotEqualTo(planVersionOneProgressNotes[0].id)
  }

  @Test
  @DisplayName("Adding Goals creates new PlanVersions")
  @WithMockUser(username = "UserId|Username")
  @Sql(scripts = [ "/db/test/plan_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  fun `test adding goals creates new plan versions correctly`() {
    // a plan and a version are added via SQL scripts in annotation

    assertThat(planVersionRepository.findAll().size).isEqualTo(1)
    assertThat(planVersionRepository.findAll().first().goals.size).isEqualTo(0)

    val goal = Goal(title = "Version testing", areaOfNeed = "Accommodation", status = GoalStatus.FUTURE)

    goalService.createNewGoal(testPlanUuid, goal)

    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0).goals.size).isEqualTo(0)
    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1).goals.size).isEqualTo(1)

    assertThat(planVersionRepository.findByUuid(testPlanVersionUuid).goals.size).isEqualTo(1)
  }

  @Test
  @DisplayName("Adding Steps creates new PlanVersions")
  @WithMockUser(username = "UserId|Username")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  fun `test adding steps creates new goals and plan versions correctly`() {
    // a plan and a version are added via SQL scripts in annotation

    // establish that we have one PlanEntity and that the initial PlanVersionEntity has two goals
    assertThat(planVersionRepository.findAll().size).isEqualTo(1)
    assertThat(planVersionRepository.findAll().first().goals.size).isEqualTo(2)

    // add a step to the goal
    val step = Step(description = "Step description", status = StepStatus.NOT_STARTED, actor = "Step actor")
    val steps: List<Step> = listOf(step)
    goalService.addStepsToGoal(UUID.fromString("31d7e986-4078-4f5c-af1d-115f9ba3722d"), Goal(steps = steps))

    // we should now have two versions, original and once made when adding steps
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0).goals.size).isEqualTo(2)
    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1).goals.size).isEqualTo(2)

    val planVersionZero = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0)
    val wholePlanVersionZero = planVersionRepository.getWholePlanVersionByUuid(planVersionZero.uuid)
    assertThat(wholePlanVersionZero.goals.first().steps.size).isEqualTo(0)

    val planVersionOne = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1)
    val wholePlanVersionOne = planVersionRepository.getWholePlanVersionByUuid(planVersionOne.uuid)
    assertThat(wholePlanVersionOne.goals.first().steps.size).isEqualTo(1)
  }

  @Test
  @DisplayName("Adding Steps to Goal with Steps creates new PlanVersions")
  @WithMockUser(username = "UserId|Username")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  fun `test adding steps to a goal with steps creates new goals and plan versions correctly`() {
    // a plan and a version are added via SQL scripts in annotation

    // add a step to the goal
    val step = Step(description = "New step description", status = StepStatus.NOT_STARTED, actor = "Step actor")
    val steps: List<Step> = listOf(step)
    goalService.addStepsToGoal(UUID.fromString("31d7e986-4078-4f5c-af1d-115f9ba3722d"), Goal(steps = steps))

    // we should now have two planversions, original and once made when adding the new step
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0).goals.size).isEqualTo(2)
    assertThat(planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1).goals.size).isEqualTo(2)

    val planVersionZero = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0)
    val wholePlanVersionZero = planVersionRepository.getWholePlanVersionByUuid(planVersionZero.uuid)
    assertThat(wholePlanVersionZero.goals.first().steps.size).isEqualTo(1)

    val planVersionOne = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1)
    val wholePlanVersionOne = planVersionRepository.getWholePlanVersionByUuid(planVersionOne.uuid)
    assertThat(wholePlanVersionOne.goals.first().steps.size).isEqualTo(2)
  }

  @Test
  @DisplayName("Agreeing a Plan creates new PlanVersions")
  @WithMockUser(username = "UserId|Username")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  fun `agreeing a plan creates new goals and plan versions correctly`() {
    // a plan and a version are added via SQL scripts in annotation

    val agreement = Agreement(
      PlanAgreementStatus.AGREED,
      "Status note",
      "Optional note",
      "Practitioner Name",
      "person Name",
    )

    planService.agreeLatestPlanVersion(testPlanUuid, agreement)

    // we should now have two PlanVersions, original and once made when agreeing the Plan
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    val planVersionZero = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0)
    val planVersionOne = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1)

    assertThat(planVersionZero.agreementNote).isNull()
    assertThat(planVersionOne.agreementNote).isNotNull
    assertThat(planVersionOne.agreementNote?.agreementStatus).isEqualTo(PlanAgreementStatus.AGREED)
  }

  @Test
  @DisplayName("Signing a Plan updates the original version and creates a new version.")
  @WithMockUser(username = "UserId|Username")
  @Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/oasys_assessment_pk_data_agreed.sql" ], executionPhase = BEFORE_TEST_METHOD)
  @Sql(scripts = [ "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_METHOD)
  fun `signing a plan creates new goals and plan versions correctly`() {
    val userDetails = UserDetails(
      id = "UserId",
      name = "Username",
    )

    val signRequest = SignRequest(
      signType = SignType.SELF,
      userDetails = userDetails,
    )

    val plan = planService.signPlan(testPlanUuid, signRequest)

    // we should now have two plan versions, original and one made before signing the Plan
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    val planVersionZero = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 0)
    val planVersionOne = planVersionRepository.getVersionByUuidAndVersion(testPlanUuid, 1)

    assertThat(planVersionZero.status).isEqualTo(CountersigningStatus.SELF_SIGNED)
    assertThat(planVersionOne.status).isEqualTo(CountersigningStatus.UNSIGNED)
    assertThat(plan.version).isEqualTo(0L)
    assertThat(plan.status).isEqualTo(CountersigningStatus.SELF_SIGNED)
  }
}
