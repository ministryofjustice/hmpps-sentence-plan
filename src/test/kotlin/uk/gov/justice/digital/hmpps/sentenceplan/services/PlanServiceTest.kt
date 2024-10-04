package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.springframework.dao.EmptyResultDataAccessException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanServiceTest {
  private val planRepository: PlanRepository = mockk()
  private val planVersionRepository: PlanVersionRepository = mockk()
  private val planAgreementNoteRepository: PlanAgreementNoteRepository = mockk()
  private val planService = PlanService(planRepository, planVersionRepository, planAgreementNoteRepository)
  private lateinit var planEntity: PlanEntity
  private lateinit var planVersionEntity: PlanVersionEntity

  @BeforeEach
  fun setup() {
    planEntity = PlanEntity(id = 0L)
    planVersionEntity = PlanVersionEntity(plan = planEntity, planId = 0L)
    planEntity.currentVersion = planVersionEntity
  }

  @Nested
  @DisplayName("getPlanVersionByPlanUuid")
  inner class GetPlanVersionByPlanUuid {

    @Test
    fun `should return plan version when plan exists with given UUID`() {
      val planUuid = UUID.randomUUID()

      every { planRepository.findByUuid(planUuid) } returns planEntity

      val result = planService.getPlanVersionByPlanUuid(planUuid)

      assertEquals(planVersionEntity, result)
    }

    @Test
    fun `should throw exception when no plan exists with given UUID`() {
      every { planRepository.findByUuid(any()) } throws EmptyResultDataAccessException(1)

      val exception = assertThrows(EmptyResultDataAccessException::class.java) {
        planService.getPlanVersionByPlanUuid(UUID.randomUUID())
      }

      assertEquals("Incorrect result size: expected 1, actual 0", exception.message)
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
    fun `should throw exception when no plan exists with given oasys assessment pk`() {
      val oasysAssessmentPk = "123456"
      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } throws EmptyResultDataAccessException(1)

      val exception = assertThrows(EmptyResultDataAccessException::class.java) {
        planService.getPlanByOasysAssessmentPk(oasysAssessmentPk)
      }

      assertEquals("Incorrect result size: expected 1, actual 0", exception.message)
    }
  }

  @Nested
  @DisplayName("createPlanByOasysAssessmentPk")
  inner class CreatePlanByOasysAssessmentPk {

    @Test
    fun `should create and return plan when no plan exists with given oasys assessment pk`() {
      val oasysAssessmentPk = "123456"
      planEntity.id = 1L

      every { planRepository.findByOasysAssessmentPk(oasysAssessmentPk) } returns null
      every { planRepository.save(any()) } returns planEntity
      every { planVersionRepository.save(any()) } returns planVersionEntity
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
          withArg { assertEquals(result.id, it) },
        )
      }

      assertEquals(planVersionEntity, result.currentVersion)
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
      every { planRepository.save(any()) } returns planEntity
      every { planVersionRepository.save(any()) } returns planVersionEntity

      val result = planService.createPlan(PlanType.INITIAL)

      verify { planRepository.save(withArg { assertEquals(result, it) }) }
    }
  }

  @Nested
  @DisplayName("agreePlan")
  inner class AgreePlan {
    private val agreement = Agreement(
      PlanAgreementStatus.AGREED,
      "Agree",
      "Agreed",
      "Tom C",
      "Pop A",
    )

    @Test
    fun `should agree plan version`() {
      every { planRepository.findByUuid(any()) } returns planEntity
      every { planVersionRepository.save(any()) } returns planVersionEntity
      every { planAgreementNoteRepository.save(any()) } returns any()

      val result = planService.agreeLatestPlanVersion(UUID.randomUUID(), agreement)

      verify(exactly = 1) { planVersionRepository.save(withArg { assertEquals(result, it) }) }
      verify(exactly = 1) { planAgreementNoteRepository.save(any()) }
    }

    @Test
    fun `should throw exception when plan already agreed`() {
      planVersionEntity.agreementStatus = PlanAgreementStatus.AGREED

      every { planRepository.findByUuid(any()) } returns planEntity

      val exception = assertThrows(ConflictException::class.java) {
        planService.agreeLatestPlanVersion(UUID.fromString("559a2111-832c-4652-a99f-eec9e570640f"), agreement)
      }

      assertEquals("Plan 559a2111-832c-4652-a99f-eec9e570640f has already been agreed.", exception.message)
    }

    @Test
    fun `should throw exception when plan not found`() {
      every { planRepository.findByUuid(any()) } throws EmptyResultDataAccessException(1)

      val exception = assertThrows(EmptyResultDataAccessException::class.java) {
        planService.agreeLatestPlanVersion(UUID.fromString("1c93ebe7-1d8d-4fcc-aef2-f97c4c983a6b"), agreement)
      }

      assertEquals("Plan was not found with UUID: 1c93ebe7-1d8d-4fcc-aef2-f97c4c983a6b", exception.message)
    }
  }
}
