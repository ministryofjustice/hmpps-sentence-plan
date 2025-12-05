package uk.gov.justice.digital.hmpps.sentenceplan.migrator.common

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity

data class User(
  val id: String = "",
  val name: String = "",
) {
  companion object {
    fun from(practitioner: PractitionerEntity?): User {
      return User(practitioner?.externalId ?: "UNKNOWN", practitioner?.username ?: "Unknown")
    }
  }
}
