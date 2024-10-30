package uk.gov.justice.digital.hmpps.sentenceplan.services

import jakarta.validation.ValidationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.CountersigningStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.getPlanByUuid
import uk.gov.justice.digital.hmpps.sentenceplan.entity.getVersionByUuidAndVersion
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CounterSignPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CountersignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.SoftDeletePlanVersionsResponse
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.time.LocalDateTime
import java.util.*

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
  private val planAgreementNoteRepository: PlanAgreementNoteRepository,
  private val versionService: VersionService,
) {

  fun getPlanVersionByPlanUuid(planUuid: UUID): PlanVersionEntity {
    val planEntity = planRepository.findByUuid(planUuid)
    return planEntity.currentVersion!!
  }

  fun rollbackVersion(planUuid: UUID, versionNumber: Int): PlanVersionEntity {
    val version = planVersionRepository.getVersionByUuidAndVersion(planUuid, versionNumber)
    version.status = CountersigningStatus.ROLLED_BACK
    return planVersionRepository.save(version)
  }

  fun clone(planUuid: UUID, planType: PlanType): PlanVersionEntity {
    val plan = planRepository.getPlanByUuid(planUuid)
    return versionService.alwaysCreateNewPlanVersion(plan.currentVersion!!).apply {
      this.planType = planType
      planVersionRepository.save(this)
    }
  }

  private fun validateRange(from: Int, to: Int, available: List<Int>, softDelete: Boolean): IntRange {
    val specifiedRange = when {
      available.isEmpty() -> throw ValidationException("No plans available or all plan versions have already had soft_deleted set to $softDelete")
      from > to -> throw ValidationException("Invalid range specified, from ($from) cannot be greater than to ($to)")
      else -> (from..to)
    }
    val availableInRange = available.filter { it in from..to }
    val unableToUpdate = specifiedRange.partition { !availableInRange.contains(it) }.first.sorted()
    if (unableToUpdate.isNotEmpty()) {
      throw ValidationException("The specified range contains version(s) (${unableToUpdate.joinToString()}) that do not exist or have already had soft_deleted set to $softDelete")
    }
    return specifiedRange
  }

  @Transactional
  fun softDelete(planUuid: UUID, from: Int, versionTo: Int?, softDelete: Boolean): SoftDeletePlanVersionsResponse {
    val plan = planRepository.getPlanByUuid(planUuid)
    val versions = planVersionRepository.findAllByPlanId(plan.id!!)
    val availableForUpdate = versions.filter { it.softDeleted != softDelete }.map { it.version }.sorted()
    val to = versionTo ?: versions.maxByOrNull { it.version }?.version ?: 0
    val range = validateRange(from, to, availableForUpdate, softDelete)
    val versionsToUpdate = versions.filter { it.version in range }.map { it.apply { it.softDeleted = softDelete } }
    planVersionRepository.saveAll(versionsToUpdate)

    if (plan.currentVersion?.version in from..to && softDelete) {
      val maxAvailableVersion = versions.filter { !it.softDeleted && it.version < from }.maxByOrNull { it.version }
        ?: versions.firstOrNull { !it.softDeleted && it.version > 0 }

      val updatedPlan = maxAvailableVersion.let { availableVersion ->
        plan.currentVersion =
          if (availableVersion != null) {
            versionService.alwaysCreateNewPlanVersion(availableVersion)
          } else {
            planVersionRepository.save(
              PlanVersionEntity(
                plan = plan,
                planId = plan.id!!,
                planType = PlanType.INITIAL,
                version = versions.maxByOrNull { it.version }?.version?.inc() ?: 0,
              ),
            )
          }
        planRepository.save(plan)
      }

      return SoftDeletePlanVersionsResponse.from(
        updatedPlan.currentVersion!!,
        planUuid,
        true,
        range.toList(),
        maxAvailableVersion?.version?.toLong(),
      )
    }
    return SoftDeletePlanVersionsResponse.from(plan.currentVersion!!, planUuid, softDelete, range.toList())
  }

  fun lockPlan(planUuid: UUID): PlanVersionEntity {
    val planEntity = planRepository.getPlanByUuid(planUuid)
    planEntity.currentVersion?.status = CountersigningStatus.LOCKED_INCOMPLETE
    planRepository.save(planEntity)

    val newVersion = versionService.alwaysCreateNewPlanVersion(planEntity.currentVersion!!)
      .apply { status = CountersigningStatus.UNSIGNED }
    planVersionRepository.save(newVersion)

    return planEntity.currentVersion!!
  }

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? =
    planRepository.findByOasysAssessmentPk(oasysAssessmentPk)

  fun createPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity {
    getPlanByOasysAssessmentPk(oasysAssessmentPk)?.let {
      throw ConflictException("Plan already associated with PK: $oasysAssessmentPk")
    }

    val planEntity = createPlan(PlanType.INITIAL)
    planRepository.createOasysAssessmentPk(oasysAssessmentPk, planEntity.id!!)
    return planEntity
  }

  fun createPlan(planType: PlanType): PlanEntity {
    val planEntity = planRepository.save(PlanEntity())

    val planVersion = PlanVersionEntity(
      plan = planEntity,
      planId = planEntity.id!!,
      planType = planType,
      version = 0,
    )

    val planVersionEntity = planVersionRepository.save(planVersion)

    planEntity.currentVersion = planVersionEntity
    planRepository.save(planEntity)

    return planEntity
  }

  @Transactional
  fun agreeLatestPlanVersion(planUuid: UUID, agreement: Agreement): PlanVersionEntity {
    val planVersion: PlanVersionEntity
    try {
      planVersion = planRepository.findByUuid(planUuid).currentVersion!!
    } catch (_: EmptyResultDataAccessException) {
      throw EmptyResultDataAccessException("Plan was not found with UUID: $planUuid", 1)
    }

    val currentPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersion)
    val agreedPlanVersion: PlanVersionEntity

    when (currentPlanVersion.agreementStatus) {
      PlanAgreementStatus.DRAFT -> {
        currentPlanVersion.agreementStatus = agreement.agreementStatus
        currentPlanVersion.agreementDate = LocalDateTime.now()
        agreedPlanVersion = planVersionRepository.save(currentPlanVersion)
        addPlanAgreementNote(agreedPlanVersion, agreement)
      }

      else -> throw ConflictException("Plan $planUuid has already been agreed.")
    }

    return agreedPlanVersion
  }

  private fun addPlanAgreementNote(planVersionEntity: PlanVersionEntity, agreement: Agreement) {
    val planAgreementNote = PlanAgreementNoteEntity(
      planVersion = planVersionEntity,
      agreementStatus = agreement.agreementStatus,
      agreementStatusNote = agreement.agreementStatusNote,
      optionalNote = agreement.optionalNote,
      practitionerName = agreement.practitionerName,
      personName = agreement.personName,
    )

    planAgreementNoteRepository.save(planAgreementNote)
  }

  /**
   * Changes the Countersigning Status of the current PlanVersion to the value of the held in the `signRequest` parameter
   * and creates a new PlanVersion with a Countersigning Status of UNSIGNED which becomes the current PlanVersion.
   */
  @Transactional
  fun signPlan(planUuid: UUID, signRequest: SignRequest): PlanVersionEntity {
    val planVersion = getPlanVersionByPlanUuid(planUuid)

    if (planVersion.agreementStatus == PlanAgreementStatus.DRAFT) {
      throw ConflictException("Plan $planUuid is in a DRAFT state, and not eligible for signing.")
    }

    versionService.alwaysCreateNewPlanVersion(planVersion)

    val signedPlan = planVersionRepository.findByPlanUuidAndVersion(planUuid, planVersion.version)

    when (signRequest.signType) {
      SignType.SELF -> {
        signedPlan.status = CountersigningStatus.SELF_SIGNED
      }

      SignType.COUNTERSIGN -> {
        signedPlan.status = CountersigningStatus.AWAITING_COUNTERSIGN
      }
    }

    planVersionRepository.save(signedPlan)

    return signedPlan
  }

  fun countersignPlan(planUuid: UUID, countersignPlanRequest: CounterSignPlanRequest): PlanVersionEntity {
    val version =
      planVersionRepository.getVersionByUuidAndVersion(planUuid, countersignPlanRequest.sentencePlanVersion.toInt())

    // Duplicate request checking
    when (countersignPlanRequest.signType) {
      CountersignType.COUNTERSIGNED -> {
        if (version.status == CountersigningStatus.COUNTERSIGNED) {
          throw ConflictException("Plan $planUuid was already countersigned.")
        }
      }

      CountersignType.REJECTED -> {
        if (version.status == CountersigningStatus.REJECTED) {
          throw ConflictException("Plan $planUuid was already rejected.")
        }
      }

      CountersignType.DOUBLE_COUNTERSIGNED -> {
        if (version.status == CountersigningStatus.DOUBLE_COUNTERSIGNED) {
          throw ConflictException("Plan $planUuid was already double countersigned.")
        }
      }

      CountersignType.AWAITING_DOUBLE_COUNTERSIGN -> {
        if (version.status == CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN) {
          throw ConflictException("Plan $planUuid was already awaiting double countersign.")
        }
      }
    }

    // Valid transitions
    when (countersignPlanRequest.signType) {
      CountersignType.COUNTERSIGNED -> {
        if (version.status != CountersigningStatus.AWAITING_COUNTERSIGN) {
          throw ConflictException("Plan $planUuid was not awaiting countersign.")
        }
        version.status = CountersigningStatus.COUNTERSIGNED
      }

      CountersignType.REJECTED -> {
        if (version.status !in arrayOf(
            CountersigningStatus.AWAITING_COUNTERSIGN,
            CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN,
          )
        ) {
          throw ConflictException("Plan $planUuid was not awaiting countersign or double countersign.")
        }
        version.status = CountersigningStatus.REJECTED
      }

      CountersignType.DOUBLE_COUNTERSIGNED -> {
        if (version.status != CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN) {
          throw ConflictException("Plan $planUuid was not awaiting double countersign.")
        }
        version.status = CountersigningStatus.DOUBLE_COUNTERSIGNED
      }

      CountersignType.AWAITING_DOUBLE_COUNTERSIGN -> {
        if (version.status != CountersigningStatus.AWAITING_COUNTERSIGN) {
          throw ConflictException("Plan $planUuid was not awaiting countersign.")
        }
        version.status = CountersigningStatus.AWAITING_DOUBLE_COUNTERSIGN
      }
    }

    return planVersionRepository.save(version)
  }
}
