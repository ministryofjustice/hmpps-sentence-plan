package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanStatus

data class Agreement(
  val agreementStatus: PlanStatus,
  val title: String,
  val text: String,
  val practitionerName: String,
  val personName: String,
)