package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType

data class ClonePlanVersionRequest(
  val planType: PlanType,
  val userDetails: UserDetails,
)
