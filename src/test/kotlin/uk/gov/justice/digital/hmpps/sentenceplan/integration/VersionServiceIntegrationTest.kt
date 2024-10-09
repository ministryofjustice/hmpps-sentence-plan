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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanProgressNotesRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
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

  // from SQL scripts
  val testPlanUuid = UUID.fromString("556db5c8-a1eb-4064-986b-0740d6a83c33")
  val testPlanVersionUuid = UUID.fromString("9f2aaa46-e544-4bcd-8db6-fbe7842ddb64")
  var newPlanVersionUuid: UUID = UUID.randomUUID()

  @Test
  @DisplayName("Create new PlanVersion")
  @WithMockUser(username = "UserId|Username")
  @Sql(
    scripts = [
      "/db/test/oasys_assessment_pk_data.sql",
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
      "/db/test/oasys_assessment_pk_cleanup.sql",
    ],
    executionPhase = AFTER_TEST_METHOD,
  )
  fun `test new plan version has the same associations as previous version`() {
    // a plan and a version are added via SQL scripts in annotation

    // create a new version
    val planVersion = planVersionRepository.findByUuid(testPlanVersionUuid)
    val planVersionOne = versionService.conditionallyCreateNewPlanVersion(planVersion)
    newPlanVersionUuid = planVersionOne.uuid

    // now check there are two versions
    assertThat(planVersionRepository.findAll().size).isEqualTo(2)

    // check that when fetched the plan references the version with ID 1
    val planEntity = planRepository.findByUuid(testPlanUuid)
    assertThat(planEntity.currentVersion!!.version).isEqualTo(1)

    // check that the UUID of version 0 is now different to the original UUID
    val planVersionZero = planVersionRepository.findByPlanUuidAndVersion(testPlanUuid, 0)
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
}
