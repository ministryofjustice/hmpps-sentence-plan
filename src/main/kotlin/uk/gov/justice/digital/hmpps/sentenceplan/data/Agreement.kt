package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus

data class Agreement(
  val agreementStatus: PlanAgreementStatus,
  val agreementStatusNote: String,
  val optionalNote: String,
  val practitionerName: String,
  val personName: String,
)
