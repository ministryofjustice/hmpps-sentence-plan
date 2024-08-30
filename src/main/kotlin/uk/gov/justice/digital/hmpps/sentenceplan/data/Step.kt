package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus

data class Step(
  val description: String,
  val status: StepStatus,
  val actor: String,
)
