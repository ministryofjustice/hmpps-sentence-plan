package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails

data class RestorePlanVersionsRequest(
  val userDetails: UserDetails,
  val from: Long,
  val to: Long?,
)
