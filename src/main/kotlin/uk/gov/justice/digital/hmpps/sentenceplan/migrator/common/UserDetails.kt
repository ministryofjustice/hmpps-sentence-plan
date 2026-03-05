package uk.gov.justice.digital.hmpps.sentenceplan.migrator.common

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity

enum class AuthSource {
  OASYS,
  HMPPS_AUTH,
  NOT_SPECIFIED,
}

data class UserDetails(
  val id: String,
  val name: String,
  val authSource: AuthSource = AuthSource.NOT_SPECIFIED,
) {
  companion object {
    fun from(practitioner: PractitionerEntity?): UserDetails = UserDetails(practitioner?.externalId ?: "UNKNOWN", practitioner?.username ?: "Unknown", AuthSource.OASYS)
  }
}
