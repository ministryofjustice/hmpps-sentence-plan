package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
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

  fun getPlanByUuid(planUuid: UUID): PlanVersionEntity? = planVersionRepository.findByUuid(planUuid)

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? =
    planRepository.findByOasysAssessmentPk(oasysAssessmentPk)

  fun createPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity {
    getPlanByOasysAssessmentPk(oasysAssessmentPk)?.let {
      throw ConflictException("Plan already associated with PK: $oasysAssessmentPk")
    }

    val plan = PlanEntity()
    val planEntity = planRepository.save(plan)
    planRepository.createOasysAssessmentPk(oasysAssessmentPk, planEntity.id!!)
    return plan
  }

  // todo where is this used and for what
  fun createPlan(): PlanEntity {
    val plan = PlanEntity()
    planRepository.save(plan)
    return plan
  }

  fun agreePlanVersion(planUuid: UUID, agreement: Agreement): PlanVersionEntity {
    var planVersion: PlanVersionEntity = getPlanByUuid(planUuid)
      ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Plan $planUuid was not found.")

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
