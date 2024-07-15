package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.data.StepActor
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorsEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val stepRepository: StepRepository = mockk()
  private val goalRepository: GoalRepository = mockk()
  private val stepActorRepository: StepActorRepository = mockk()
  private val areaOfNeedRepository: AreaOfNeedRepository = mockk()
  private val currentTime = LocalDateTime.now().toString()
  private val goalService = GoalService(goalRepository, stepRepository, stepActorRepository, areaOfNeedRepository)
  private val uuid = UUID.fromString("ef74ee4b-5a0b-481b-860f-19187260f2e7")

  private val stepEntity = StepEntity(
    description = "description",
    id = 123L,
    status = "status",
    relatedGoalUuid = UUID.fromString("ef74ee4b-5a0b-481b-860f-19187260f2e7"),
    creationDate = currentTime,
  )
  private val actorsEntityList = listOf(
    StepActorsEntity(1, UUID.fromString("71793b64-545e-4ae7-9936-610639093857"), "actor", 1),
  )
  private val actors = listOf(
    StepActor("actor", 1),
  )
  private val steps = Step(
    description = "description",
    status = "status",
    actor = actors,
  )

  @Test
  fun `create new steps`() {
    every { stepRepository.save(any<StepEntity>()) } returns stepEntity
    every { stepActorRepository.saveAll(any<List<StepActorsEntity>>()) } returns actorsEntityList
    val stepsList = goalService.createNewSteps(uuid, listOf(steps))
    assertThat(stepsList.get(0).status).isEqualTo("status")
    assertThat(stepsList.get(0).id).isEqualTo(123)
    assertThat(stepsList.get(0).relatedGoalUuid).isEqualTo(uuid)
    assertThat(stepsList.get(0).description).isEqualTo("description")
    assertThat(stepsList.get(0).creationDate).isEqualTo(currentTime)
  }
}
