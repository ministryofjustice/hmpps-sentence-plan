package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementNoteRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanStatus
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.time.Instant
import java.util.UUID

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val planAgreementNoteRepository: PlanAgreementNoteRepository,
) {

  fun getPlanByUuid(planUuid: UUID): PlanEntity? = planRepository.findByUuid(planUuid)

  fun getPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity? =
    planRepository.findByOasysAssessmentPk(oasysAssessmentPk)

  fun createPlanByOasysAssessmentPk(oasysAssessmentPk: String): PlanEntity {
    getPlanByOasysAssessmentPk(oasysAssessmentPk)?.let {
      throw ConflictException("Plan already associated with PK: $oasysAssessmentPk")
    }

    val plan = PlanEntity()
    planRepository.save(plan)
    planRepository.createOasysAssessmentPk(oasysAssessmentPk, plan.uuid)
    return plan
  }

  fun createPlan(): PlanEntity {
    val plan = PlanEntity()
    planRepository.save(plan)
    return plan
  }

  fun agreePlan(planUuid: UUID, agreement: Agreement): PlanEntity {
    val plan: PlanEntity = getPlanByUuid(planUuid)
      ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Plan $planUuid was not found.")

    when (plan.agreementStatus) {
      PlanStatus.DRAFT -> {
        val updatedDate = Instant.now()
        plan.agreementStatus = agreement.agreementStatus
        plan.agreementDate = updatedDate
        planRepository.save(plan)
        addPlanAgreementNote(planUuid, agreement)
      }
      else -> throw ConflictException("Plan $planUuid has already been agreed.")
    }

    return plan
  }

  fun addPlanAgreementNote(planUuid: UUID, agreement: Agreement) {
    val entity = PlanAgreementNoteEntity(
      planUuid = planUuid,
      agreementStatus = agreement.agreementStatus,
      agreementStatusNote = agreement.agreementStatusNote,
      optionalNote = agreement.optionalNote,
      practitionerName = agreement.practitionerName,
      personName = agreement.personName,
    )

    planAgreementNoteRepository.save(entity)
  }
}
