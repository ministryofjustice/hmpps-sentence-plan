package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails

data class LockRequest(
  val lockType: LockType,
  val userDetails: UserDetails,
)

enum class LockType {
  SELF,
  COUNTERSIGN,
}
