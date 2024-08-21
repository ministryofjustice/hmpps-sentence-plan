package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanProgressNotesRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanStatus
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.util.UUID

class PlanServiceTest {

  private val planRepository: PlanRepository = mockk()
  private val planProgressNotesRepository: PlanProgressNotesRepository = mockk()
  private val planService = PlanService(planRepository, planProgressNotesRepository)

  @Nested
  @DisplayName("gePlayByUuid")
  inner class GetPlanByUuid {

    @Test
    fun `should return plan when plan exists with given UUID`() {
      val planUuid = UUID.randomUUID()
      val planEntity = PlanEntity()
      every { planRepository.findByUuid(planUuid) } returns planEntity

      val result = planService.getPlanByUuid(planUuid)

      assertEquals(planEntity, result)
    }

    @Test
    fun `should return null when no plan exists with given UUID`() {
      val planUuid = UUID.randomUUID()
      every { planRepository.findByUuid(planUuid) } returns null

      val result = planService.getPlanByUuid(planUuid)

      assertNull(result)
    }
  }

  @Nested
  @DisplayName("getPlanByOasysAssessmentPk")
  inner class GetPlanByOasysAssessmentPk {

    @Test
    fun `should return plan when plan exists with given oasys assessment pk`() {
      val oasysAssessmentPk = "123456"
      val planEntity = PlanEntity()
      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } returns planEntity

      val result = planService.getPlanByOasysAssessmentPk(oasysAssessmentPk)

      assertEquals(planEntity, result)
    }

    @Test
    fun `should return null when no plan exists with given oasys assessment pk`() {
      val oasysAssessmentPk = "123456"
      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } returns null

      val result = planService.getPlanByOasysAssessmentPk(oasysAssessmentPk)

      assertNull(result)
    }
  }

  @Nested
  @DisplayName("createPlanByOasysAssessmentPk")
  inner class CreatePlanByOasysAssessmentPk {

    @Test
    fun `should create and return plan when no plan exists with given oasys assessment pk`() {
      val oasysAssessmentPk = "123456"
      val planEntity = PlanEntity()

      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } returns null
      every { planRepository.save(any()) } returns planEntity
      every { planRepository.createOasysAssessmentPk(oasysAssessmentPk, any()) } returns Unit

      val result = planService.createPlanByOasysAssessmentPk(oasysAssessmentPk)

      verify {
        planRepository.save(
          withArg {
            assertEquals(result.uuid, it.uuid)
          },
        )
      }

      verify {
        planRepository.createOasysAssessmentPk(
          withArg { assertEquals(oasysAssessmentPk, it) },
          withArg { assertEquals(result.uuid, it) },
        )
      }
    }

    @Test
    fun `should throw ConflictException when plan already exists with given oasys assessment PK`() {
      val oasysAssessmentPk = "123456"
      val existingPlan = PlanEntity()
      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } returns existingPlan

      val exception = assertThrows(ConflictException::class.java) {
        planService.createPlanByOasysAssessmentPk(oasysAssessmentPk)
      }

      assertEquals("Plan already associated with PK: $oasysAssessmentPk", exception.message)
    }
  }

  @Nested
  @DisplayName("createPlan")
  inner class CreatePlan {

    @Test
    fun `should create and return a new plan`() {
      every { planRepository.save(any()) } returns any()

      val result = planService.createPlan()

      verify { planRepository.save(withArg { assertEquals(result, it) }) }
    }
  }

  @Nested
  @DisplayName("agreePlan")
  inner class AgreePlan {
    private val agreement = Agreement(
      PlanStatus.AGREED,
      "Agree",
      "Agreed",
      "Tom C",
      "Pop A",
    )

    @Test
    fun `should agree plan`() {
      every { planRepository.save(any()) } returns any()
      every { planRepository.findByUuid(any()) } returns PlanEntity()
      every { planProgressNotesRepository.save(any()) } returns any()

      val result = planService.agreePlan(UUID.randomUUID(), agreement)

      verify(exactly = 1) { planRepository.save(withArg { assertEquals(result, it) }) }
      verify(exactly = 1) { planProgressNotesRepository.save(any()) }
    }

    @Test
    fun `should throw exception when plan already agreed`() {
      val planEntity: PlanEntity = PlanEntity(agreementStatus = PlanStatus.AGREED)
      every { planRepository.findByUuid(any()) } returns planEntity

      val exception = assertThrows(ConflictException::class.java) {
        planService.agreePlan(UUID.fromString("559a2111-832c-4652-a99f-eec9e570640f"), agreement)
      }

      assertEquals("Plan 559a2111-832c-4652-a99f-eec9e570640f has already been agreed.", exception.message)
    }

    @Test
    fun `should throw exception when plan not found`() {
      every { planRepository.findByUuid(any()) } returns any()

      val exception = assertThrows(ResponseStatusException::class.java) {
        planService.agreePlan(UUID.randomUUID(), agreement)
      }

      assertEquals("422 UNPROCESSABLE_ENTITY", exception.message)
    }
  }
}
