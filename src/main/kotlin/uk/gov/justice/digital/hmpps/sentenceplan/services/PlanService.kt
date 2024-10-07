package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.LockRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.LockType
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.time.LocalDateTime
import java.util.UUID

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
  private val planAgreementNoteRepository: PlanAgreementNoteRepository,
) {

  @Autowired
  private lateinit var versionService: VersionService

  fun getPlanVersionByPlanUuid(planUuid: UUID): PlanVersionEntity {
    val planEntity = planRepository.findByUuid(planUuid)
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
    } catch (e: EmptyResultDataAccessException) {
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

  fun signPlan(planUuid: UUID, lockRequest: LockRequest): PlanVersionEntity {
    val plan = getPlanVersionByPlanUuid(planUuid)

    // Check signing status here
    if (plan.status != CountersigningStatus.UNSIGNED) {
      println(plan) // Placeholder for throw?
    }

    when (lockRequest.lockType) {
      LockType.SELF -> { plan.status = CountersigningStatus.SELF_SIGNED }
      LockType.COUNTERSIGN -> { plan.status = CountersigningStatus.AWAITING_COUNTERSIGN }
    }
    // make a new version in the UNSIGNED state
    val versionedPlan = versionService.createNewPlanVersion(planUuid)
    versionedPlan.status = CountersigningStatus.UNSIGNED
    planVersionRepository.save(versionedPlan)

    // make sure we update the previous version with the new status, not the new one.
    return planVersionRepository.save(plan)
  }
}
