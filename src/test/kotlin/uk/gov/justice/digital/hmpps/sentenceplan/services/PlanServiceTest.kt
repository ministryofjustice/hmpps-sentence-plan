package uk.gov.justice.digital.hmpps.sentenceplan.services

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.CountersigningStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CounterSignPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CountersignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import java.util.UUID
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlanServiceTest {

  private val planRepository: PlanRepository = mockk()
  private val planVersionRepository: PlanVersionRepository = mockk(relaxed = true)
  private val planAgreementNoteRepository: PlanAgreementNoteRepository = mockk()
  private val versionService: VersionService = mockk<VersionService>(relaxed = true)
  private val planService = PlanService(planRepository, planVersionRepository, planAgreementNoteRepository, versionService)
  private lateinit var planEntity: PlanEntity
  private lateinit var planVersionEntity: PlanVersionEntity
  private lateinit var newPlanVersionEntity: PlanVersionEntity
  private lateinit var agreedNewPlanVersionEntity: PlanVersionEntity
  private lateinit var currentCouldNotAnswerPlanVersionEntity: PlanVersionEntity
  private lateinit var updatedAgreePlanVersionEntity: PlanVersionEntity
  private lateinit var planVersionEntities: List<PlanVersionEntity>

  @BeforeEach
  fun setup() {
    clearMocks(planRepository, planVersionRepository, planAgreementNoteRepository, versionService)
    planEntity = PlanEntity(id = 0L)
    planVersionEntity = PlanVersionEntity(id = 0, plan = planEntity, planId = 0L, version = 0)
    newPlanVersionEntity = PlanVersionEntity(id = 1, plan = planEntity, planId = 0L, version = 1)
    agreedNewPlanVersionEntity = PlanVersionEntity(
      id = 1,
      plan = planEntity,
      planId = 0L,
      version = 1,
      agreementStatus = PlanAgreementStatus.AGREED,
    )
    currentCouldNotAnswerPlanVersionEntity = PlanVersionEntity(
      id = 1,
      plan = planEntity,
      planId = 0L,
      version = 1,
      agreementStatus = PlanAgreementStatus.COULD_NOT_ANSWER,
    )
    updatedAgreePlanVersionEntity = PlanVersionEntity(
      id = 1,
      plan = planEntity,
      planId = 0L,
      version = 1,
      agreementStatus = PlanAgreementStatus.UPDATED_AGREED,
    )
    planEntity.currentVersion = planVersionEntity
  }

  @Nested
  @DisplayName("getPlanVersionByPlanUuid")
  inner class GetPlanVersionByPlanUuid {

    @Test
    fun `should return plan version when plan exists with given UUID`() {
      val planUuid = UUID.randomUUID()

      every { planRepository.getByUuid(planUuid) } returns planEntity

      val result = planService.getPlanVersionByPlanUuid(planUuid)

      assertEquals(planVersionEntity, result)
    }

    @Test
    fun `should throw exception when no plan exists with given UUID`() {
      val planUuid = UUID.randomUUID()
      every { planRepository.getByUuid(planUuid) } throws NotFoundException("Plan not found for id $planUuid")

      assertThrows<NotFoundException> {
        planService.getPlanVersionByPlanUuid(planUuid)
      }
    }
  }

  @Nested
  @DisplayName("getPlanVersionByPlanUuidAndPlanVersion")
  inner class GetPlanVersionByPlanUuidAndPlanVersion {

    @Test
    fun `should return plan version when plan exists with given UUID and version`() {
      val planUuid = UUID.randomUUID()
      val planVersion = 1

      every { planVersionRepository.getVersionByUuidAndVersion(planUuid, planVersion) } returns planVersionEntity

      val result = planService.getPlanVersionByPlanUuidAndPlanVersion(planUuid, planVersion)

      assertEquals(planVersionEntity, result)
    }
  }

  @Nested
  @DisplayName("getPlanVersionsByPlanUuid")
  inner class GetPlanVersionsByPlanUuid {

    @Test
    fun `should return all plan versions when plan exists with given UUID`() {
      planVersionEntities = listOf(planVersionEntity, newPlanVersionEntity)

      every { planRepository.getByUuid(planEntity.uuid) } returns planEntity
      every { planVersionRepository.findAllByPlanId(planEntity.id!!) } returns planVersionEntities

      val result = planService.getPlanVersionsByPlanUuid(planEntity.uuid)

      assertEquals(planVersionEntities, result)

      verify(exactly = 1) { planRepository.getByUuid(planEntity.uuid) }
      verify(exactly = 1) { planVersionRepository.findAllByPlanId(planEntity.id!!) }
    }

    @Test
    fun `should throw an exception when plan does not exist`() {
      planVersionEntities = listOf(planVersionEntity, newPlanVersionEntity)

      every { planRepository.getByUuid(planEntity.uuid) } throws NotFoundException("plan not found")

      val exception = assertThrows(NotFoundException::class.java) {
        planService.getPlanVersionsByPlanUuid(planEntity.uuid)
      }

      assertEquals("plan not found", exception.message)

      verify(exactly = 1) { planRepository.getByUuid(planEntity.uuid) }
      verify(exactly = 0) { planVersionRepository.findAllByPlanId(planEntity.id!!) }
    }
  }

  @Nested
  @DisplayName("getPlanVersionByVersionUuid")
  inner class GetPlanVersionByVersionUuid {

    @Test
    fun `should return plan version if one exists with given version UUID`() {
      val planVersionUuid = UUID.randomUUID()

      every { planVersionRepository.findByUuid(planVersionUuid) } returns planVersionEntity

      val result = planService.getPlanVersionByVersionUuid(planVersionUuid)

      assertEquals(planVersionEntity, result)
    }

    @Test
    fun `should throw an exception when plan version does not exist`() {
      val planVersionUuid = UUID.randomUUID()

      every { planVersionRepository.findByUuid(planVersionUuid) } throws NotFoundException("Could not find a plan version with ID: $planVersionUuid")

      val exception = assertThrows(NotFoundException::class.java) {
        planService.getPlanVersionByVersionUuid(planVersionUuid)
      }

      assertThrows<NotFoundException> {
        planService.getPlanVersionByVersionUuid(planVersionUuid)
      }
      assertEquals("Could not find a plan version with ID: $planVersionUuid", exception.message)
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
      every { planRepository.getByUuid(any()) } returns planEntity
      every { planVersionRepository.save(planVersionEntity) } returns planVersionEntity
      every { planVersionRepository.save(newPlanVersionEntity) } returns agreedNewPlanVersionEntity
      every { planAgreementNoteRepository.save(any()) } returns any()
      every { versionService.conditionallyCreateNewPlanVersion(planVersionEntity) } returns newPlanVersionEntity

      val result = planService.agreeLatestPlanVersion(UUID.randomUUID(), agreement)

      verify(exactly = 1) { planAgreementNoteRepository.save(any()) }
      assertThat(result.version).isEqualTo(newPlanVersionEntity.version)
      assertThat(result.agreementStatus).isEqualTo(agreement.agreementStatus)
    }

    @Test
    fun `should agree plan when current status is could not answer`() {
      planEntity.currentVersion = currentCouldNotAnswerPlanVersionEntity

      val agreement = Agreement(
        agreementStatus = PlanAgreementStatus.UPDATED_AGREED,
        agreementStatusNote = "Agree",
        practitionerName = "Tom C",
        optionalNote = "",
        personName = "Pop A",
      )

      every { planRepository.getByUuid(any()) } returns planEntity
      every { versionService.conditionallyCreateNewPlanVersion(currentCouldNotAnswerPlanVersionEntity) } returns currentCouldNotAnswerPlanVersionEntity
      every { planVersionRepository.save(currentCouldNotAnswerPlanVersionEntity) } returns updatedAgreePlanVersionEntity
      every { planAgreementNoteRepository.save(any()) } returns any()

      val result = planService.agreeLatestPlanVersion(UUID.randomUUID(), agreement)

      verify(exactly = 1) { planAgreementNoteRepository.save(any()) }
      assertThat(result.version).isEqualTo(updatedAgreePlanVersionEntity.version)
      assertThat(result.agreementStatus).isEqualTo(PlanAgreementStatus.UPDATED_AGREED)
    }

    @Test
    fun `should throw exception when plan already agreed`() {
      planVersionEntity.agreementStatus = PlanAgreementStatus.AGREED

      every { planRepository.getByUuid(any()) } returns planEntity
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns agreedNewPlanVersionEntity

      val exception = assertThrows(ConflictException::class.java) {
        planService.agreeLatestPlanVersion(UUID.fromString("559a2111-832c-4652-a99f-eec9e570640f"), agreement)
      }

      assertEquals("Plan 559a2111-832c-4652-a99f-eec9e570640f has already been agreed.", exception.message)
    }

    @Test
    fun `should throw exception when plan not found`() {
      every { planRepository.getByUuid(any()) } throws NotFoundException("Plan not found for id 1c93ebe7-1d8d-4fcc-aef2-f97c4c983a6b")

      val exception = assertThrows(NotFoundException::class.java) {
        planService.agreeLatestPlanVersion(UUID.fromString("1c93ebe7-1d8d-4fcc-aef2-f97c4c983a6b"), agreement)
      }

      assertEquals("Plan not found for id 1c93ebe7-1d8d-4fcc-aef2-f97c4c983a6b", exception.message)
    }
  }

  @Nested
  @DisplayName("signPlan")
  inner class SignPlan {
    val userDetails = UserDetails(
      id = "123",
      name = "Tom C",
    )

    @Test
    fun `should mark plan as self-signed`() {
      every { planRepository.getByUuid(any()) } returns planEntity.apply { currentVersion?.agreementStatus = PlanAgreementStatus.AGREED }
      every { planVersionRepository.save(any()) } returnsArgument 0
      every { planVersionRepository.getVersionByUuidAndVersion(any(), any()) } returns newPlanVersionEntity
      every { versionService.alwaysCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val signRequest = SignRequest(
        signType = SignType.SELF,
        userDetails = userDetails,
      )

      val plan = planService.signPlan(UUID.randomUUID(), signRequest)

      assertThat(plan.status).isEqualTo(CountersigningStatus.SELF_SIGNED)
    }

    @Test
    fun `should mark plan as awaiting-countersign`() {
      every { planRepository.getByUuid(any()) } returns planEntity.apply { currentVersion?.agreementStatus = PlanAgreementStatus.AGREED }
      every { planVersionRepository.save(any()) } returnsArgument 0
      every { planVersionRepository.getVersionByUuidAndVersion(any(), any()) } returns newPlanVersionEntity
      every { versionService.conditionallyCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val signRequest = SignRequest(
        signType = SignType.COUNTERSIGN,
        userDetails = userDetails,
      )

      val plan = planService.signPlan(UUID.randomUUID(), signRequest)

      assertThat(plan.status).isEqualTo(CountersigningStatus.AWAITING_COUNTERSIGN)
    }

    @Test
    fun `should prevent signing, as plan is in a draft state`() {
      every { planRepository.getByUuid(any()) } returns planEntity
      every { planVersionRepository.save(any()) } returnsArgument 0
      every { versionService.alwaysCreateNewPlanVersion(any()) } returns newPlanVersionEntity

      val signRequest = SignRequest(
        signType = SignType.SELF,
        userDetails = userDetails,
      )

      val exception = assertThrows(ConflictException::class.java) {
        planService.signPlan(UUID.randomUUID(), signRequest)
      }

      assertThat(exception.message).endsWith("is in a DRAFT state, and not eligible for signing.")
    }
  }

  @Nested
  @DisplayName("countersignPlan")
  inner class CountersignPlan {

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.sentenceplan.services.PlanServiceTest#statusAlreadyMatches")
    fun `should throw exception if status already matches`(type: CountersignType, status: CountersigningStatus, ending: String) {
      every { planVersionRepository.getVersionByUuidAndVersion(any(), any()) } returns newPlanVersionEntity.apply { this.status = status }

      val request = CounterSignPlanRequest(
        signType = type,
        sentencePlanVersion = 0L,
      )

      val exception = assertThrows(ConflictException::class.java) {
        planService.countersignPlan(UUID.randomUUID(), request)
      }

      assertThat(exception.message).endsWith(ending)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.sentenceplan.services.PlanServiceTest#validStateTransitions")
    fun `valid requests based on given state`(type: CountersignType, initialStatus: CountersigningStatus, finalStatus: CountersigningStatus) {
      every { planVersionRepository.getVersionByUuidAndVersion(any(), any()) } returns newPlanVersionEntity.apply { this.status = initialStatus }
      every { planVersionRepository.save(any()) } returnsArgument 0

      val request = CounterSignPlanRequest(
        signType = type,
        sentencePlanVersion = 0L,
      )

      val version = planService.countersignPlan(UUID.randomUUID(), request)

      assertThat(version.status).isEqualTo(finalStatus)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.sentenceplan.services.PlanServiceTest#invalidStateTransitions")
    fun `invalid requests based on given state`(type: CountersignType, status: CountersigningStatus, ending: String) {
      every { planVersionRepository.getVersionByUuidAndVersion(any(), any()) } returns newPlanVersionEntity.apply { this.status = status }
      every { planVersionRepository.save(any()) } returnsArgument 0

      val request = CounterSignPlanRequest(
        signType = type,
        sentencePlanVersion = 0L,
      )

      val exception = assertThrows(ConflictException::class.java) {
        planService.countersignPlan(UUID.randomUUID(), request)
      }

      assertThat(exception.message).endsWith(ending)
    }
  }

  private companion object {
    @JvmStatic
    fun statusAlreadyMatches(): Stream<Arguments> = Stream.of(
      Arguments.of(CountersignType.COUNTERSIGNED, CountersigningStatus.COUNTERSIGNED, "was already countersigned."),
      Arguments.of(CountersignType.REJECTED, CountersigningStatus.REJECTED, "was already rejected."),
      Arguments.of(
        CountersignType.DOUBLE_COUNTERSIGNED,
        CountersigningStatus.DOUBLE_COUNTERSIGNED,
        "was already double countersigned.",
      ),
      Arguments.of(
        CountersignType.AWAITING_DOUBLE_COUNTERSIGN,
        CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
        "was already awaiting double countersign.",
      ),
    )

    @JvmStatic
    fun validStateTransitions(): Stream<Arguments> = Stream.of(
      Arguments.of(
        CountersignType.COUNTERSIGNED,
        CountersigningStatus.AWAITING_COUNTERSIGN,
        CountersigningStatus.COUNTERSIGNED,
      ),
      Arguments.of(CountersignType.REJECTED, CountersigningStatus.AWAITING_COUNTERSIGN, CountersigningStatus.REJECTED),
      Arguments.of(
        CountersignType.REJECTED,
        CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
        CountersigningStatus.REJECTED,
      ),
      Arguments.of(
        CountersignType.DOUBLE_COUNTERSIGNED,
        CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
        CountersigningStatus.DOUBLE_COUNTERSIGNED,
      ),
      Arguments.of(
        CountersignType.AWAITING_DOUBLE_COUNTERSIGN,
        CountersigningStatus.AWAITING_COUNTERSIGN,
        CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
      ),
    )

    @JvmStatic
    fun invalidStateTransitions(): Stream<Arguments> {
      val list = mutableListOf<Arguments>()
      for (status in CountersigningStatus.entries.filter {
        it !in arrayOf(
          CountersigningStatus.AWAITING_COUNTERSIGN,
          CountersigningStatus.COUNTERSIGNED,
        )
      }) {
        list.add(Arguments.of(CountersignType.COUNTERSIGNED, status, "was not awaiting countersign."))
      }
      for (status in CountersigningStatus.entries.filter {
        it !in arrayOf(
          CountersigningStatus.AWAITING_COUNTERSIGN,
          CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
          CountersigningStatus.REJECTED,
        )
      }) {
        list.add(Arguments.of(CountersignType.REJECTED, status, "was not awaiting countersign or double countersign."))
      }
      for (status in CountersigningStatus.entries.filter {
        it !in arrayOf(
          CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
          CountersigningStatus.DOUBLE_COUNTERSIGNED,
        )
      }) {
        list.add(Arguments.of(CountersignType.DOUBLE_COUNTERSIGNED, status, "was not awaiting double countersign."))
      }
      for (status in CountersigningStatus.entries.filter {
        it !in arrayOf(
          CountersigningStatus.AWAITING_COUNTERSIGN,
          CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
        )
      }) {
        list.add(
          Arguments.of(
            CountersignType.AWAITING_DOUBLE_COUNTERSIGN,
            status,
            "was not awaiting countersign.",
          ),
        )
      }
      return list.stream()
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("softDelete and restore")
  inner class SoftDeletedPlan {

    @ParameterizedTest
    @MethodSource("softDeleteExceptionTestData")
    fun `should throw exception `(versions: List<Int>, from: Int, to: Int?, softDelete: Boolean, expected: String) {
      planVersionEntities = (0..9).toList().map { PlanVersionEntity(plan = planEntity, planId = 1L, version = it, softDeleted = softDelete) }
      planVersionEntities.forEach {
        if (versions.contains(it.version)) {
          it.softDeleted = !softDelete
        }
      }
      planEntity.apply { currentVersion = planVersionEntities.filter { !it.softDeleted }.maxByOrNull { it.version } }
      every { planRepository.getByUuid(any()) } returns planEntity
      every { planRepository.save(any()) } returns planEntity
      every { planVersionRepository.findAllByPlanId(any()) } returns planVersionEntities
      val result = assertThrows<ValidationException> { planService.softDelete(UUID.randomUUID(), from, to, softDelete) }
      assertThat(result.message).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("softDeleteSuccessTestData")
    fun `should successfully set soft_delete `(versions: List<Int>, from: Int, to: Int?, softDelete: Boolean, expected: List<Int>) {
      planVersionEntities = (0..9).toList().map { PlanVersionEntity(plan = planEntity, planId = 1L, version = it, softDeleted = softDelete) }
      planVersionEntities.forEach {
        if (versions.contains(it.version)) {
          it.apply { it.softDeleted = !softDelete }
        }
      }
      planEntity.apply { currentVersion = planVersionEntities.filter { !it.softDeleted }.maxByOrNull { it.version } }
      every { planRepository.getByUuid(any()) } returns planEntity
      every { planRepository.save(any()) } returns planEntity
      every { planVersionRepository.findAllByPlanId(any()) } returns planVersionEntities
      every { planVersionRepository.findFirstByPlanIdAndSoftDeletedOrderByVersionDesc(any(), false) } returns planVersionEntities.reversed().firstOrNull { !it.softDeleted }
      val result = planService.softDelete(UUID.randomUUID(), from, to, softDelete)
      if (softDelete) {
        assertThat(result?.versionsSoftDeleted).isEqualTo(expected)
      } else {
        assertThat(result?.versionsRestored).isEqualTo(expected)
      }
    }
    private fun softDeleteExceptionTestData() = listOf(
      Arguments.of(listOf(3, 4, 5, 7), 2, 1, true, "Invalid range specified, from (2) must be lower than to (1)"),
      Arguments.of(listOf(3, 4, 5, 7), 2, 2, true, "Invalid range specified, from (2) must be lower than to (2)"),
      Arguments.of(listOf(3, 4, 5, 7), 2, 5, true, "The specified range contains version(s) (2) that do not exist or have already had soft_deleted set to true"),
      Arguments.of(listOf(3, 4, 5, 7), 3, 7, true, "The specified range contains version(s) (6) that do not exist or have already had soft_deleted set to true"),
      Arguments.of(listOf(3, 4, 5, 7), 0, 8, true, "The specified range contains version(s) (0, 1, 2, 6) that do not exist or have already had soft_deleted set to true"),
      Arguments.of(listOf(3, 4, 5, 7), 3, null, true, "The specified range contains version(s) (6, 8, 9) that do not exist or have already had soft_deleted set to true"),
      Arguments.of(listOf(3, 4, 5, 6, 8, 9), 4, 8, true, "The specified range contains version(s) (7) that do not exist or have already had soft_deleted set to true"),
      Arguments.of(emptyList<Int>(), 4, 8, true, "No plans available or all plan versions have already had soft_deleted set to true"),
//      // undelete
      Arguments.of(listOf(3, 4, 5, 7), 2, 1, false, "Invalid range specified, from (2) must be lower than to (1)"),
      Arguments.of(listOf(3, 4, 5, 7), 2, 2, false, "Invalid range specified, from (2) must be lower than to (2)"),
      Arguments.of(listOf(3, 4, 5, 7), 2, 5, false, "The specified range contains version(s) (2) that do not exist or have already had soft_deleted set to false"),
      Arguments.of(listOf(3, 4, 5, 7), 3, 7, false, "The specified range contains version(s) (6) that do not exist or have already had soft_deleted set to false"),
      Arguments.of(listOf(3, 4, 5, 7), 0, 8, false, "The specified range contains version(s) (0, 1, 2, 6) that do not exist or have already had soft_deleted set to false"),
      Arguments.of(listOf(3, 4, 5, 7), 3, null, false, "The specified range contains version(s) (6, 8, 9) that do not exist or have already had soft_deleted set to false"),
      Arguments.of(listOf(3, 4, 5, 6, 8, 9), 4, 8, false, "The specified range contains version(s) (7) that do not exist or have already had soft_deleted set to false"),
      Arguments.of(emptyList<Int>(), 4, 8, false, "No plans available or all plan versions have already had soft_deleted set to false"),
    )
    private fun softDeleteSuccessTestData() = listOf(
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 3, null, true, listOf(3, 4, 5, 6, 7, 8, 9)),
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 4, 8, true, listOf(4, 5, 6, 7)),
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 4, 5, true, listOf(4)),
      // undelete
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 3, null, false, listOf(3, 4, 5, 6, 7, 8, 9)),
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 4, 8, false, listOf(4, 5, 6, 7)),
      Arguments.of(listOf(3, 4, 5, 6, 7, 8, 9), 4, 5, false, listOf(4)),
    )
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("clone plan version")
  inner class ClonePlanVersion {

    @Test
    fun `should create a new version of the plan with a new version`() {
      val planUUID = planEntity.uuid
      val updatedPlanEntity = planEntity.apply {
        currentVersion = PlanVersionEntity(
          id = 1,
          plan = planEntity,
          planId = 0L,
          version = planEntity.currentVersion!!.version.inc(),
        )
      }
      every { planRepository.getByUuid(any()) } returns planEntity
      every { versionService.alwaysCreateNewPlanVersion(any()) } returns updatedPlanEntity.currentVersion!!
      every { planVersionRepository.save(any()) } returns updatedPlanEntity.currentVersion
      val result = planService.clone(planUUID, PlanType.OTHER)
      assertThat(result.version).isEqualTo(1L)
      assertThat(result.planType).isEqualTo(PlanType.OTHER)
    }
  }
}
