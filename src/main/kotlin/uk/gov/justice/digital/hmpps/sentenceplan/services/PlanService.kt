package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.entity.*
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
    val plan = getPlanByUuid(planUuid)

    if (plan?.agreementStatus == PlanStatus.DRAFT) {
      plan.agreementStatus = agreement.agreementStatus
      planRepository.save(plan)
      addPlanProgressNote(planUuid, agreement)
    }

    return plan!!
  }

  fun addPlanProgressNote(planUuid: UUID, agreement: Agreement) {
    val entity = PlanProgressNoteEntity(
      planUuid = planUuid,
      title = agreement.title,
      text = agreement.text,
      practitioner_name = agreement.practitionerName,
      person_name = agreement.personName
    )
    planProgressNotesRepository.save(entity)
  }
}
