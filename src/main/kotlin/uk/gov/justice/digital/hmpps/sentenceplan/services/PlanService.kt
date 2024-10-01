package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.time.LocalDateTime
import java.util.UUID

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
  private val planAgreementNoteRepository: PlanAgreementNoteRepository,
) {

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

    val plan = PlanEntity()
    val planEntity = planRepository.save(plan)

    val planVersion = PlanVersionEntity(plan = planEntity)
    val planVersionEntity = planVersionRepository.save(planVersion)

    planEntity.currentVersion = planVersionEntity
    planRepository.save(planEntity)

    planRepository.createOasysAssessmentPk(oasysAssessmentPk, planEntity.id!!)
    return planEntity
  }

  fun createPlan(planType: PlanType): PlanEntity {
    val plan = PlanEntity()
    // TODO: Set planType & planVersion
    val planEntity = planRepository.save(plan)

    val planVersion = PlanVersionEntity(plan = planEntity)
    planVersionRepository.save(planVersion)

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
}
