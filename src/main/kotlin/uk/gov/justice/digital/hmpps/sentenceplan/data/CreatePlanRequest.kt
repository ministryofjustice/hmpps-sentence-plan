package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType

data class CreatePlanRequest(
  val planType: PlanType,
)
// Maybe it includes Auditing information...