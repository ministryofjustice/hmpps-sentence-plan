package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.EmptyResultDataAccessException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val goalRepository: GoalRepository = mockk()
  private val areaOfNeedRepository: AreaOfNeedRepository = mockk()
  private val planVersionRepository: PlanVersionRepository = mockk()
  private val stepRepository: StepRepository = mockk()
  private val goalService = GoalService(goalRepository, areaOfNeedRepository, planVersionRepository, stepRepository)
  private val goalUuid = UUID.fromString("ef74ee4b-5a0b-481b-860f-19187260f2e7")
  private val plan: PlanEntity = mockk()

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

  private val goalEntityNoSteps: GoalEntity = GoalEntity(
    title = "Mock Goal",
    areaOfNeed = mockk<AreaOfNeedEntity>(),
    planVersion = null,
    uuid = goalUuid,
    goalOrder = 1,
  )

  private val goalEntityWithRelatedAreasOfNeed: GoalEntity = GoalEntity(
    title = "Mock Goal with Related Areas of Need",
    areaOfNeed = mockk<AreaOfNeedEntity>(),
    planVersion = null,
    uuid = goalUuid,
    goalOrder = 1,
    relatedAreasOfNeed = mockk<MutableList<AreaOfNeedEntity>>(),
  )

  private val planVersionEntity: PlanVersionEntity = PlanVersionEntity(plan = plan)

  private val goalSet = setOf(goalEntityNoSteps)

  private val planVersionEntityWithOneGoal: PlanVersionEntity = PlanVersionEntity(plan = plan, goals = goalSet)

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

  @Nested
  @DisplayName("createNewGoal")
  inner class CreateNewGoal {

    @Test
    fun `create new goal with random Plan UUID should throw Exception`() {
      every { planVersionRepository.findByUuid(any()) } throws EmptyResultDataAccessException(1)

      val exception = assertThrows<Exception> {
        goalService.createNewGoal(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("A Plan with this UUID was not found:")
    }

    @Test
    fun `create new goal with Area Of Need that doesn't exist should throw Exception`() {
      every { planVersionRepository.findByUuid(any()) } returns planVersionEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns null

      var exception: Exception? = null
      var goalEntity: GoalEntity? = null

      try {
        goalEntity = goalService.createNewGoal(planVersionEntity.uuid, goal)
      } catch (e: Exception) {
        exception = e
      }

      assertThat(goalEntity).isNull()
      assertThat(exception).isNotNull()
      assertThat(exception?.message).isEqualTo("An Area of Need with this name was not found: Area Of Need")
    }

    @Test
    fun `create new goal with Related Areas Of Need that don't exist should throw Exception`() {
      every { planVersionRepository.findByUuid(any()) } returns planVersionEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null

      var exception: Exception? = null
      var goalEntity: GoalEntity? = null

      try {
        goalEntity = goalService.createNewGoal(planVersionEntity.uuid, goal)
      } catch (e: Exception) {
        exception = e
      }

      assertThat(goalEntity).isNull()
      assertThat(exception).isNotNull()
      assertThat(exception?.message).startsWith("One or more of the Related Areas of Need was not found:")
    }

    @Test
    fun `create new goal with no Related Areas of Need should call save`() {
      every { planVersionRepository.findByUuid(any()) } returns planVersionEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntity = goalService.createNewGoal(planVersionEntity.uuid, goalWithNoRelatedAreasOfNeed)

      assertThat(goalEntity).isNotNull()
      assertThat(goalEntity.relatedAreasOfNeed).isEmpty()
    }

    @Test
    fun `creating two goals should set incrementing goal order values`() {
      every { planVersionRepository.findByUuid(any()) } returns planVersionEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntityOne = goalService.createNewGoal(planVersionEntity.uuid, goalWithNoRelatedAreasOfNeed)
      assertThat(goalEntityOne).isNotNull()
      assertThat(goalEntityOne.goalOrder).isEqualTo(1)

      every { planVersionRepository.findByUuid(any()) } returns planVersionEntityWithOneGoal

      val goalEntityTwo = goalService.createNewGoal(planVersionEntity.uuid, goalWithNoRelatedAreasOfNeed)
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

      val stepsList = goalService.addStepsToGoal(goalUuid, steps)

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
  @DisplayName("UpdateGoal")
  inner class UpdateGoal {

    @Test
    fun `update goal with random Plan UUID should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns null

      val exception = assertThrows<Exception> {
        goalService.updateGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("This Goal was not found:")
    }

    @Test
    fun `update goal with related areas of need`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity)

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val savedGoal: GoalEntity = goalService.updateGoalByUuid(UUID.randomUUID(), goal)

      assertThat(savedGoal.title).isEqualTo(goal.title)
    }

    @Test
    fun `update goal with related areas of need not found should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns null

      val exception = assertThrows<Exception> {
        goalService.updateGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("One or more of the Related Areas of Need was not found:")
    }

    @Test
    fun `update goal with unmatched related areas of need should throw Exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityWithRelatedAreasOfNeed
      every { areaOfNeedRepository.findAllByNames(any()) } returns listOf(areaOfNeedEntity, areaOfNeedEntity)

      val exception = assertThrows<Exception> {
        goalService.updateGoalByUuid(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("One or more of the Related Areas of Need was not found")
    }
  }

  @Nested
  @DisplayName("UpdateSteps")
  inner class UpdateSteps {
    @Test
    fun `update steps for goal that does not exist should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns null

      val exception = assertThrows<Exception> {
        goalService.addStepsToGoal(UUID.randomUUID(), steps, true)
      }

      assertThat(exception.message).startsWith("This Goal was not found:")
    }

    @Test
    fun `update steps with an empty list should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps

      val exception = assertThrows<IllegalArgumentException> {
        goalService.addStepsToGoal(UUID.randomUUID(), emptyList(), true)
      }

      assertThat(exception.message).startsWith("At least one Step must be provided")
    }

    @Test
    fun `update steps where a step is incomplete should throw an exception`() {
      every { goalRepository.findByUuid(any()) } returns goalEntityNoSteps

      val exception = assertThrows<IllegalArgumentException> {
        goalService.addStepsToGoal(UUID.randomUUID(), incompleteSteps, true)
      }

      assertThat(exception.message).startsWith("All Steps must contain all the required information")
    }

    @Test
    fun `update steps for goal with no steps should return the new steps`() {
      val goalSlot = slot<GoalEntity>()
      every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }
      every { stepRepository.deleteAll(any()) } returns Unit

      val stepsList = goalService.addStepsToGoal(goalUuid, steps, true)

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

      val stepsList = goalService.addStepsToGoal(goalUuid, steps, true)

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
}
