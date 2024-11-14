package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.EmptyResultDataAccessException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalStatusUpdate
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalServiceTest {
  private val goalRepository: GoalRepository = mockk()
  private val areaOfNeedRepository: AreaOfNeedRepository = mockk()
  private val planRepository: PlanRepository = mockk()
  private val stepRepository: StepRepository = mockk()
  private val versionService: VersionService = mockk()

  private val goalService = GoalService(goalRepository, areaOfNeedRepository, stepRepository, planRepository, versionService)
  private val goalUuid = UUID.fromString("ef74ee4b-5a0b-481b-860f-19187260f2e7")

  private val goal: Goal = Goal(
    areaOfNeed = "Area Of Need",
    title = "Goal",
    relatedAreasOfNeed = listOf("Related Area of Need 1"),
  )

  private val goalWithNoRelatedAreasOfNeed: Goal = Goal(
    areaOfNeed = "Area Of Need",
    title = "Goal",
  )

  private val areaOfNeedEntity: AreaOfNeedEntity = AreaOfNeedEntity(
    id = null,
    name = "Area of Need",
    uuid = UUID.randomUUID(),
    goals = emptyList(),
  )

  private lateinit var goalEntityNoSteps: GoalEntity

  private lateinit var goalSet: Set<GoalEntity>

  private val goalEntityWithRelatedAreasOfNeed: GoalEntity = GoalEntity(
    title = "Mock Goal with Related Areas of Need",
    areaOfNeed = mockk<AreaOfNeedEntity>(),
    planVersion = null,
    uuid = goalUuid,
    goalOrder = 1,
    relatedAreasOfNeed = mutableSetOf(areaOfNeedEntity),
  )

  private val steps = listOf(
    Step(
      description = "description 1",
      status = StepStatus.CANNOT_BE_DONE_YET,
      actor = "actor 1",
    ),
    Step(
      description = "description 2",
      status = StepStatus.NOT_STARTED,
      actor = "actor 2",
    ),
  )

  private val incompleteSteps = steps + Step("This is a step with no actor", status = StepStatus.NOT_STARTED, actor = "")

  private val planEntity: PlanEntity = PlanEntity()
  private val planVersionEntity: PlanVersionEntity = PlanVersionEntity(plan = planEntity, planId = 0L)
  private val newPlanVersionEntity: PlanVersionEntity = PlanVersionEntity(plan = planEntity, planId = 1L)
  private lateinit var planVersionEntityWithOneGoal: PlanVersionEntity

  @BeforeAll
  fun setupBeforeAll() {
    planEntity.currentVersion = planVersionEntity
  }

  @BeforeEach
  fun setupBeforeEach() {
    goalEntityNoSteps = GoalEntity(
      title = "Mock Goal",
      areaOfNeed = mockk<AreaOfNeedEntity>(),
      planVersion = null,
      uuid = goalUuid,
      goalOrder = 1,
      status = GoalStatus.ACTIVE,
      relatedAreasOfNeed = mutableSetOf(areaOfNeedEntity),
    )

    goalSet = setOf(goalEntityNoSteps)

    planVersionEntityWithOneGoal = PlanVersionEntity(plan = planEntity, goals = goalSet, planId = 0L)
  }

  @Nested
  @DisplayName("createNewGoal")
  inner class CreateNewGoal {

    @Test
    fun `create new goal with random Plan UUID should throw Exception`() {
      every { planRepository.findByUuid(any()) } throws EmptyResultDataAccessException(1)

      val exception = assertThrows<Exception> {
        goalService.createNewGoal(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("A Plan with this UUID was not found:")
    }

    @Test
    fun `create new goal with Area Of Need that doesn't exist should throw Exception`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } throws EmptyResultDataAccessException(1)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      var exception: Exception? = null
      var goalEntity: GoalEntity? = null

      try {
        goalEntity = goalService.createNewGoal(planEntity.uuid, goal)
      } catch (e: Exception) {
        exception = e
      }

      assertThat(goalEntity).isNull()
      assertThat(exception).isNotNull()
      assertThat(exception?.message).isEqualTo("An Area of Need with this name was not found: Area Of Need")
    }

    @Test
    fun `create new goal with Related Areas Of Need that don't exist should throw Exception`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      var exception: Exception? = null
      var goalEntity: GoalEntity? = null

      try {
        goalEntity = goalService.createNewGoal(planEntity.uuid, goal)
      } catch (e: Exception) {
        exception = e
      }

      assertThat(goalEntity).isNull()
      assertThat(exception).isNotNull()
      assertThat(exception?.message).startsWith("One or more of the Related Areas of Need was not found:")
    }

    @Test
    fun `create new goal with no Related Areas of Need should call save`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntity = goalService.createNewGoal(planVersionEntity.uuid, goalWithNoRelatedAreasOfNeed)

      assertThat(goalEntity).isNotNull()
      assertThat(goalEntity.relatedAreasOfNeed).isEmpty()
    }

    @Test
    fun `creating two goals should set incrementing goal order values`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntityOne = goalService.createNewGoal(planEntity.uuid, goalWithNoRelatedAreasOfNeed)
      assertThat(goalEntityOne).isNotNull()
      assertThat(goalEntityOne.goalOrder).isEqualTo(1)

      planEntity.currentVersion = planVersionEntityWithOneGoal

      val goalEntityTwo = goalService.createNewGoal(planEntity.uuid, goalWithNoRelatedAreasOfNeed)
      assertThat(goalEntityTwo).isNotNull()
      assertThat(goalEntityTwo.goalOrder).isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("createNewSteps")
  inner class CreateNewSteps {

    @Test
    fun `should create new steps`() {
      val goalSlot = slot<GoalEntity>()
      every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val stepsList = goalService.addStepsToGoal(goalUuid, Goal(steps = steps))

      assertThat(stepsList.size).isEqualTo(2)

      assertThat(stepsList.first().status).isEqualTo(StepStatus.CANNOT_BE_DONE_YET)
      assertThat(stepsList.first().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.first().description).isEqualTo("description 1")
      assertThat(stepsList.first().actor).isEqualTo("actor 1")

      assertThat(stepsList.last().status).isEqualTo(StepStatus.NOT_STARTED)
      assertThat(stepsList.last().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.last().description).isEqualTo("description 2")
    }
  }

  @Nested
  @DisplayName("ReplaceGoal")
  inner class ReplaceGoal {

    @Test
    fun `update goal with random Plan UUID should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<Exception> {
        goalService.replaceGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("This Goal was not found:")
    }

    @Test
    fun `update goal with related areas of need`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val savedGoal: GoalEntity = goalService.replaceGoalByUuid(UUID.randomUUID(), goal)

      assertThat(savedGoal.title).isEqualTo(goal.title)
    }

    @Test
    fun `update goal with related areas of need not found should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<Exception> {
        goalService.replaceGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("One or more of the Related Areas of Need was not found:")
    }

    @Test
    fun `update goal with unmatched related areas of need should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity, areaOfNeedEntity)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<Exception> {
        goalService.replaceGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("One or more of the Related Areas of Need was not found")
    }

    @Test
    fun `update goal with new note and no note type should add note with Type PROGRESS`() {
      val goal = Goal(
        title = "Goal title",
        areaOfNeed = "Finances",
        note = "Simple note update",
      )

      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val savedGoal: GoalEntity = goalService.replaceGoalByUuid(UUID.randomUUID(), goal)

      assertThat(savedGoal.notes.first().note).isEqualTo(goal.note)
      assertThat(savedGoal.notes.first().type).isEqualTo(GoalNoteType.PROGRESS)
      assertThat(savedGoal.status).isEqualTo(GoalStatus.ACTIVE)
    }

    @Test
    fun `update goal with new note and status REMOVED should add note with Type REMOVED`() {
      val goal = Goal(
        title = "A title",
        note = "Simple note update",
        areaOfNeed = "Finances",
        status = GoalStatus.REMOVED,
      )

      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val savedGoal: GoalEntity = goalService.replaceGoalByUuid(UUID.randomUUID(), goal)

      assertThat(savedGoal.notes.first().note).isEqualTo(goal.note)
      assertThat(savedGoal.notes.first().type).isEqualTo(GoalNoteType.REMOVED)
      assertThat(savedGoal.status).isEqualTo(GoalStatus.REMOVED)
    }
  }

  @Nested
  @DisplayName("UpdateGoal")
  inner class UpdateGoal {
    @Test
    fun `update goal with new note and status ACHIEVED should add note with Type ACHIEVED and not remove Related Areas of Need`() {
      val goalStatusUpdate = GoalStatusUpdate(
        note = "Simple note update",
        status = GoalStatus.ACHIEVED,
      )

      every { goalRepository.getGoalByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity)
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val savedGoal = goalService.updateGoalStatus(UUID.randomUUID(), goalStatusUpdate)

      assertThat(savedGoal.notes.first().note).isEqualTo("Simple note update")
      assertThat(savedGoal.notes.first().type).isEqualTo(GoalNoteType.ACHIEVED)
      assertThat(savedGoal.status).isEqualTo(GoalStatus.ACHIEVED)
      assertThat(savedGoal.relatedAreasOfNeed?.size).isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("UpdateSteps")
  inner class UpdateSteps {
    @Test
    fun `update steps for goal that does not exist should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns null
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<Exception> {
        goalService.addStepsToGoal(UUID.randomUUID(), Goal(steps = steps), true)
      }

      assertThat(exception.message).startsWith("This Goal was not found:")
    }

    @Test
    fun `update steps with an empty list should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<IllegalArgumentException> {
        goalService.addStepsToGoal(UUID.randomUUID(), Goal(steps = emptyList()), true)
      }

      assertThat(exception.message).startsWith("A Step or Note must be provided")
    }

    @Test
    fun `update steps where a step is incomplete should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val exception = assertThrows<IllegalArgumentException> {
        goalService.addStepsToGoal(UUID.randomUUID(), Goal(steps = incompleteSteps), true)
      }

      assertThat(exception.message).startsWith("All Steps must contain all the required information")
    }

    @Test
    fun `update steps for goal with no steps should return the new steps`() {
      val goalSlot = slot<GoalEntity>()
      every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }
      every { stepRepository.deleteAll(any()) } returns Unit
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val stepsList = goalService.addStepsToGoal(goalUuid, Goal(steps = steps), true)

      assertThat(stepsList.size).isEqualTo(2)

      assertThat(stepsList.first().status).isEqualTo(StepStatus.CANNOT_BE_DONE_YET)
      assertThat(stepsList.first().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.first().description).isEqualTo("description 1")
      assertThat(stepsList.first().actor).isEqualTo("actor 1")

      assertThat(stepsList.last().status).isEqualTo(StepStatus.NOT_STARTED)
      assertThat(stepsList.last().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.last().description).isEqualTo("description 2")
    }

    @Test
    fun `update steps for goal with an existing step only returns the new steps`() {
      val goalSlot = slot<GoalEntity>()

      val goalEntityWithOneStep = GoalEntity(
        title = "Mock Goal",
        areaOfNeed = mockk<AreaOfNeedEntity>(),
        planVersion = null,
        uuid = goalUuid,
        goalOrder = 1,
      )
      goalEntityWithOneStep.steps = listOf(
        StepEntity(
          description = "Initial step description",
          status = StepStatus.NOT_STARTED,
          actor = "Initial actor",
          goal = goalEntityWithOneStep,
        ),
      )

      every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }
      every { stepRepository.deleteAll(any()) } returns Unit
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val stepsList = goalService.addStepsToGoal(goalUuid, Goal(steps = steps), true)

      assertThat(stepsList.size).isEqualTo(2)

      assertThat(stepsList.first().status).isEqualTo(StepStatus.CANNOT_BE_DONE_YET)
      assertThat(stepsList.first().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.first().description).isEqualTo("description 1")
      assertThat(stepsList.first().actor).isEqualTo("actor 1")

      assertThat(stepsList.last().status).isEqualTo(StepStatus.NOT_STARTED)
      assertThat(stepsList.last().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.last().description).isEqualTo("description 2")
    }

    @Test
    fun `update steps with no steps and a note saves the note`() {
      val goalSlot = slot<GoalEntity>()

      val goalEntityWithNoSteps = GoalEntity(
        title = "Mock Goal",
        areaOfNeed = mockk<AreaOfNeedEntity>(),
        planVersion = null,
        uuid = goalUuid,
        goalOrder = 1,
      )

      val noteToAdd = "This is a new note"

      every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }
      every { stepRepository.deleteAll(any()) } returns Unit
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val stepsList = goalService.addStepsToGoal(goalUuid, Goal(note = noteToAdd), true)

      assertThat(stepsList.size).isEqualTo(0)
      assertThat(goalSlot.captured.notes.size).isEqualTo(1)
      assertThat(goalSlot.captured.notes.first().note).isEqualTo(noteToAdd)
    }
  }
}
