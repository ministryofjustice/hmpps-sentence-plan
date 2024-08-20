package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanProgressNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanProgressNotesRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanStatus
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import java.util.UUID

@Service
class PlanService(
  private val planRepository: PlanRepository,
  private val planProgressNotesRepository: PlanProgressNotesRepository,
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
    val plan: PlanEntity = getPlanByUuid(planUuid) ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY)

    when(plan.agreementStatus) {
      PlanStatus.DRAFT -> {
        plan.agreementStatus = agreement.agreementStatus
        planRepository.save(plan)
        addPlanProgressNote(planUuid, agreement)
      }
      else -> throw ConflictException("Plan $planUuid has already been agreed.")
    }

    return plan
  }

  fun addPlanProgressNote(planUuid: UUID, agreement: Agreement) {
    val entity = PlanProgressNoteEntity(
      planUuid = planUuid,
      title = agreement.title,
      text = agreement.text,
      practitioner_name = agreement.practitionerName,
      person_name = agreement.personName,
    )
    planProgressNotesRepository.save(entity)
  }
}
