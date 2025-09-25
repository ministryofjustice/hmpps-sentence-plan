package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionServiceTest {

  private lateinit var planEntity: PlanEntity
  private lateinit var planVersionEntity: PlanVersionEntity
  private lateinit var newPlanVersionEntity: PlanVersionEntity

  private val planVersionRepository: PlanVersionRepository = mockk()

  private var versionService = spyk(VersionService(planVersionRepository), recordPrivateCalls = true)

  @BeforeEach
  fun setup() {
    planEntity = PlanEntity(id = 0L)
    planVersionEntity = PlanVersionEntity(plan = planEntity, id = 0, planId = 0L, version = 0)
    newPlanVersionEntity = PlanVersionEntity(plan = planEntity, id = 1, planId = 0L, version = 1)
    planEntity.currentVersion = planVersionEntity

    every { planVersionRepository.getWholePlanVersionByUuid(any()) } returns planVersionEntity
    every { planVersionRepository.findByUuid(any()) } returns planVersionEntity
    every { planVersionRepository.save(any()) } returns newPlanVersionEntity
    every { planVersionRepository.findNextPlanVersion(any()) } returns newPlanVersionEntity.version.inc()

    versionService.entityManager = mockk<EntityManager>(relaxed = true)
  }

  @Test
  fun `should not make a new version if the last updated date was today`() {
    val returnedPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersionEntity)

    assertThat(returnedPlanVersion.version).isEqualTo(0)
  }

  @Test
  fun `should make a new version if the last updated date was yesterday`() {
    planVersionEntity.updatedDate = LocalDateTime.now().minusDays(1)

    val returnedPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersionEntity)

    assertThat(returnedPlanVersion.version).isEqualTo(newPlanVersionEntity.version)
  }

  @Test
  fun `should make a new version if the last updated date was last year`() {
    planVersionEntity.updatedDate = LocalDateTime.now().minusYears(1)

    val returnedPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersionEntity)

    assertThat(returnedPlanVersion.version).isEqualTo(newPlanVersionEntity.version)
  }

  @Nested
  @DisplayName("getPlanVersionByVersionUuid")
  inner class GetPlanVersionByVersionUuid {

    @Test
    fun `should return plan version if one exists with given version UUID`() {
      val planVersionUuid = UUID.randomUUID()
      val result = versionService.getPlanVersionByVersionUuid(planVersionUuid)

      assertEquals(planVersionEntity, result)
    }

    @Test
    fun `should throw an exception when plan version does not exist`() {
      val planVersionUuid = UUID.randomUUID()
      every { planVersionRepository.findByUuid(planVersionUuid) } throws NotFoundException("Could not find a plan version with ID: $planVersionUuid")

      val exception = assertThrows(NotFoundException::class.java) {
        versionService.getPlanVersionByVersionUuid(planVersionUuid)
      }

      assertEquals("Could not find a plan version with ID: $planVersionUuid", exception.message)
    }
  }
}
