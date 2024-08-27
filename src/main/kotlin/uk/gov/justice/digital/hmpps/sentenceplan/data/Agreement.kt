package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanStatus

data class Agreement(
  val agreementStatus: PlanStatus,
  val agreementStatusNote: String,
  val optionalNote: String,
  val practitionerName: String,
  val personName: String,
)
