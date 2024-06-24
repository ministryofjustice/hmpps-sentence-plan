package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val stepRepository: StepRepository = mockk()
  private val goalRepository: GoalRepository = mockk()
  private val currentTime = LocalDateTime.now().toString()
  private val goalService = GoalService(goalRepository, stepRepository)
  val uuid = UUID.randomUUID()
  val goalEntity = GoalEntity(
    id = 123L,
    title = "title",
    areaOfNeed = "area",
    targetDate = currentTime,
    goalOrder = 1,
    planUuid = UUID.randomUUID(),
  )
  val stepEntity = StepEntity(
    description = "description",
    id = 123L,
    relatedGoalId = uuid,
    actor = "actor",
    status = "status",
    creationDate = currentTime,
  )

  @Test
  fun `create new goal`() {
    every { goalRepository.save(any()) } returns goalEntity
    val goal = goalService.createNewGoal(goalEntity)
    assertThat(goal.uuid).isNotNull()
    assertThat(goal.id).isEqualTo(123)
    assertThat(goal.title).isEqualTo("title")
    assertThat(goal.targetDate).isEqualTo(currentTime)
  }

  @Test
  fun `create new step`() {
    every { stepRepository.saveAll(any<List<StepEntity>>()) } returns listOf(stepEntity)
    val stepsList = goalService.createNewStep(listOf(stepEntity), uuid)
    assertThat(stepsList.get(0).status).isEqualTo("status")
    assertThat(stepsList.get(0).id).isEqualTo(123)
    assertThat(stepsList.get(0).relatedGoalId).isEqualTo(uuid)
    assertThat(stepsList.get(0).actor).isEqualTo("actor")
    assertThat(stepsList.get(0).description).isEqualTo("description")
    assertThat(stepsList.get(0).creationDate).isEqualTo(currentTime)
  }

  @Test
  fun `get all goals`() {
    every { goalRepository.findAll() } returns listOf(goalEntity)
    val goalList = goalService.getAllGoals()
    assertThat(goalList.size).isEqualTo(1)
    assertThat(goalList[0]).isEqualTo(goalEntity)
  }

  @Test
  fun `get all goal steps`() {
    every { stepRepository.findAllByRelatedGoalId(uuid) } returns Optional.of(listOf(stepEntity))
    val stepList = goalService.getAllGoalSteps(uuid)
    assertThat(stepList.size).isEqualTo(1)
    assertThat(stepList[0]).isEqualTo(stepEntity)
  }
}
