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
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.data.StepActor
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val goalRepository: GoalRepository = mockk()
  private val areaOfNeedRepository: AreaOfNeedRepository = mockk()
  private val planRepository: PlanRepository = mockk()
  private val goalService = GoalService(goalRepository, areaOfNeedRepository, planRepository)
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

  private val goalEntityNoSteps: GoalEntity = GoalEntity(
    title = "Mock Goal",
    areaOfNeed = mockk<AreaOfNeedEntity>(),
    plan = null,
    uuid = goalUuid,
    goalOrder = 1,
  )

  private val planEntity: PlanEntity = PlanEntity()

  private val goalSet = setOf(goalEntityNoSteps)

  private val planEntityWithOneGoal: PlanEntity = PlanEntity(goals = goalSet)

  private val actors = listOf(
    StepActor("actor 1", 1),
    StepActor("actor 2", 2),
  )

  private val steps = listOf(
    Step(
      description = "description 1",
      status = "status 1",
      actor = actors,
    ),
    Step(
      description = "description 2",
      status = "status 2",
      actor = actors,
    ),
  )

  @Nested
  @DisplayName("createNewGoal")
  inner class CreateNewGoal {

    @Test
    fun `create new goal with random Plan UUID should throw Exception`() {
      every { planRepository.findByUuid(any()) } returns null

      val exception = assertThrows<Exception> {
        goalService.createNewGoal(UUID.randomUUID(), goal)
      }

      assertThat(exception.message).startsWith("A Plan with this UUID was not found:")
    }

    @Test
    fun `create new goal with Area Of Need that doesn't exist should throw Exception`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns null

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

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntity = goalService.createNewGoal(planEntity.uuid, goalWithNoRelatedAreasOfNeed)

      assertThat(goalEntity).isNotNull()
      assertThat(goalEntity.relatedAreasOfNeed).isEmpty()
    }

    @Test
    fun `creating two goals should set incrementing goal order values`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { areaOfNeedRepository.findByNameIgnoreCase(any()) } returns areaOfNeedEntity
      every { areaOfNeedRepository.findAllByNames(any()) } returns null

      val goalSlot = slot<GoalEntity>()
      every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

      val goalEntityOne = goalService.createNewGoal(planEntity.uuid, goalWithNoRelatedAreasOfNeed)
      assertThat(goalEntityOne).isNotNull()
      assertThat(goalEntityOne.goalOrder).isEqualTo(1)

      every { planRepository.findByUuid(any()) } returns planEntityWithOneGoal

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

      val stepsList = goalService.createNewSteps(goalUuid, steps).steps

      assertThat(stepsList.size).isEqualTo(2)

      assertThat(stepsList.first().status).isEqualTo("status 1")
      assertThat(stepsList.first().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.first().description).isEqualTo("description 1")
      assertThat(stepsList.first().actors.size).isEqualTo(2)
      assertThat(stepsList.first().actors.first().actor).isEqualTo("actor 1")
      assertThat(stepsList.first().actors.last().actor).isEqualTo("actor 2")

      assertThat(stepsList.last().status).isEqualTo("status 2")
      assertThat(stepsList.last().goal?.uuid).isEqualTo(goalUuid)
      assertThat(stepsList.last().description).isEqualTo("description 2")
    }
  }
}
