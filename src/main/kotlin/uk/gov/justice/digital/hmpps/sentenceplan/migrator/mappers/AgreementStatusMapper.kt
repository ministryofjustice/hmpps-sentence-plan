package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus

class AgreementStatusMapper {
  companion object {
    fun map(status: PlanAgreementStatus) = when (status) {
      PlanAgreementStatus.DRAFT -> "DRAFT"
      PlanAgreementStatus.AGREED -> "AGREED"
      PlanAgreementStatus.DO_NOT_AGREE -> "DO_NOT_AGREE"
      PlanAgreementStatus.COULD_NOT_ANSWER -> "COULD_NOT_ANSWER"
      PlanAgreementStatus.UPDATED_AGREED -> "UPDATED_AGREED"
      PlanAgreementStatus.UPDATED_DO_NOT_AGREE -> "UPDATED_DO_NOT_AGREE"
    }
  }
}