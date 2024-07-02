package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val stepRepository: StepRepository = mockk()
  private val goalRepository: GoalRepository = mockk()
  private val currentTime = LocalDateTime.now().toString()
  private val goalService = GoalService(goalRepository, stepRepository)
  private val uuid = UUID.randomUUID()

  val stepEntity = StepEntity(
    description = "description",
    id = 123L,
    actor = "actor",
    status = "status",
    creationDate = currentTime,
  )

  @Test
  fun `add related goal UUID to list of steps`() {
    val stepsList = goalService.addRelatedGoalUuidToSteps(uuid, listOf(stepEntity))
    assertThat(stepsList.get(0).relatedGoalUuid).isEqualTo(uuid)
  }

  @Test
  fun `create new step`() {
    every { stepRepository.saveAll(any<List<StepEntity>>()) } returns listOf(stepEntity)
    val stepsList = goalService.createNewStep(uuid, listOf(stepEntity))
    assertThat(stepsList.get(0).status).isEqualTo("status")
    assertThat(stepsList.get(0).id).isEqualTo(123)
    assertThat(stepsList.get(0).relatedGoalUuid).isEqualTo(uuid)
    assertThat(stepsList.get(0).actor).isEqualTo("actor")
    assertThat(stepsList.get(0).description).isEqualTo("description")
    assertThat(stepsList.get(0).creationDate).isEqualTo(currentTime)
  }
}
