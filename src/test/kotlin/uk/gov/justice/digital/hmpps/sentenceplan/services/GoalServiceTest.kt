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
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Gaol Service Tests")
class GoalServiceTest {
  private val stepRepository: StepRepository = mockk()
  private val goalRepository: GoalRepository = mockk()
  private val currentTime = LocalDateTime.now().toString()
  private val goalService = GoalService(goalRepository, stepRepository)

  @Test
  fun `create new goal`() {
    val goalEntity = GoalEntity(
      agreementNote = "note",
      id = 123L,
      title = "title",
      areaOfNeed = "area",
      targetDate = currentTime,
      isAgreed = true,
      goalOrder = 1,
    )
    every { goalRepository.save(any()) } returns goalEntity
    val goal = goalService.createNewGoal(goalEntity)
    assertThat(goal.uuid).isNotNull()
    assertThat(goal.id).isEqualTo(123)
    assertThat(goal.title).isEqualTo("title")
    assertThat(goal.agreementNote).isEqualTo("note")
    assertThat(goal.targetDate).isEqualTo(currentTime)
    assertThat(goal.isAgreed).isEqualTo(true)
  }

  @Test
  fun `create new step`() {
    val uuid = UUID.randomUUID()
    val stepEntity = StepEntity(
      description = "description",
      id = 123L,
      relatedGoalId = uuid,
      actor = "actor",
      status = "status",
      creationDate = currentTime,
    )
    every { stepRepository.saveAll(any<List<StepEntity>>()) } returns listOf(stepEntity)
    val stepsList = goalService.createNewStep(listOf(stepEntity), uuid)
    assertThat(stepsList.get(0).status).isEqualTo("status")
    assertThat(stepsList.get(0).id).isEqualTo(123)
    assertThat(stepsList.get(0).relatedGoalId).isEqualTo(uuid)
    assertThat(stepsList.get(0).actor).isEqualTo("actor")
    assertThat(stepsList.get(0).description).isEqualTo("description")
    assertThat(stepsList.get(0).creationDate).isEqualTo(currentTime)
  }
}
