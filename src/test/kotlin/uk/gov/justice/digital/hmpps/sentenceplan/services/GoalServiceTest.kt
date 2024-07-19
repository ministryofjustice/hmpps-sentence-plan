package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.data.StepActor
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorsEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("Goal Service Tests")
class GoalServiceTest {
  private val stepRepository: StepRepository = mockk()
  private val goalRepository: GoalRepository = mockk()
  private val stepActorRepository: StepActorRepository = mockk()
  private val areaOfNeedRepository: AreaOfNeedRepository = mockk()
  private val planRepository: PlanRepository = mockk()
  private val goalService = GoalService(goalRepository, stepRepository, stepActorRepository, areaOfNeedRepository, planRepository)
  private val goalUuid = UUID.fromString("ef74ee4b-5a0b-481b-860f-19187260f2e7")

  private val goalEntityNoSteps: GoalEntity = GoalEntity(
    title = "Mock Goal",
    areaOfNeedUuid = UUID.randomUUID(),
    plan = null,
    uuid = goalUuid,
  )

  private val actorsEntityList = listOf(
    StepActorsEntity(1, UUID.fromString("71793b64-545e-4ae7-9936-610639093857"), "actor", 1),
  )
  private val actors = listOf(
    StepActor("actor", 1),
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

  @Test
  fun `create new steps`() {
    val goalSlot = slot<GoalEntity>()
    every { goalRepository.findByUuid(goalUuid) } returns goalEntityNoSteps
    every { goalRepository.save(capture(goalSlot)) } answers { goalSlot.captured }

    val stepsList = goalService.createNewSteps(goalUuid, steps).steps!!

    assertThat(stepsList.size).isEqualTo(2)

    assertThat(stepsList.get(0).status).isEqualTo("status 1")
    assertThat(stepsList.get(0).goal?.uuid).isEqualTo(goalUuid)
    assertThat(stepsList.get(0).description).isEqualTo("description 1")

    assertThat(stepsList.get(1).status).isEqualTo("status 2")
    assertThat(stepsList.get(1).goal?.uuid).isEqualTo(goalUuid)
    assertThat(stepsList.get(1).description).isEqualTo("description 2")
  }
}
