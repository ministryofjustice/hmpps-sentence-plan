package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails

data class DeletePlanRequest(
  val userDetails: UserDetails,
)
