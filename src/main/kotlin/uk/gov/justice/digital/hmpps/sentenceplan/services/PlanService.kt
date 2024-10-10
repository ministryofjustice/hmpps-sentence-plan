package uk.gov.justice.digital.hmpps.sentenceplan.services

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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignType
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.time.LocalDateTime
import java.util.UUID

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

  fun lockPlan(planUuid: UUID): PlanVersionEntity {
    val planEntity = planRepository.getPlanByUuid(planUuid)
    planEntity.currentVersion?.status = CountersigningStatus.LOCKED_INCOMPLETE
    planRepository.save(planEntity)

    val newVersion = versionService.alwaysCreateNewPlanVersion(planEntity.currentVersion!!).apply { status = CountersigningStatus.UNSIGNED }
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

  fun agreeLatestPlanVersion(planUuid: UUID, agreement: Agreement): PlanVersionEntity {
    var planVersion: PlanVersionEntity
    try {
      planVersion = planRepository.findByUuid(planUuid).currentVersion!!
    } catch (_: EmptyResultDataAccessException) {
      throw EmptyResultDataAccessException("Plan was not found with UUID: $planUuid", 1)
    }

    when (planVersion.agreementStatus) {
      PlanAgreementStatus.DRAFT -> {
        planVersion.agreementStatus = agreement.agreementStatus
        planVersion.agreementDate = LocalDateTime.now()
        planVersion = planVersionRepository.save(planVersion)
        addPlanAgreementNote(planVersion, agreement)
      }
      else -> throw ConflictException("Plan $planUuid has already been agreed.")
    }

    return planVersion
  }

  fun addPlanAgreementNote(planVersionEntity: PlanVersionEntity, agreement: Agreement) {
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

  @Transactional
  fun signPlan(planUuid: UUID, signRequest: SignRequest): PlanVersionEntity {
    val plan = getPlanVersionByPlanUuid(planUuid)

    when (signRequest.signType) {
      SignType.SELF -> {
        plan.status = CountersigningStatus.SELF_SIGNED
      }
      SignType.COUNTERSIGN -> {
        plan.status = CountersigningStatus.AWAITING_COUNTERSIGN
      }
    }

    // make a new version in the UNSIGNED state
    val versionedPlan = versionService.alwaysCreateNewPlanVersion(plan)
      .apply {
        status = CountersigningStatus.UNSIGNED
      }

    planVersionRepository.save(versionedPlan)

    // make sure we update the previous version with the new status, not the new one.
    return planVersionRepository.save(plan)
  }
}
