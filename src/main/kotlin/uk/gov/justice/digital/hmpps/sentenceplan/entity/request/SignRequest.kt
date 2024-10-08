package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails

data class SignRequest(
  val signType: SignType,
  val userDetails: UserDetails,
)

enum class SignType {
  SELF,
  COUNTERSIGN,
}
