package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

class ActorsMapper {
  companion object {
    fun map(value: String) = when (value) {
      "Probation practitioner" -> "probation_practitioner"
      "Prison offender manager" -> "prison_offender_manager"
      "Programme staff" -> "programme_staff"
      "Partnership agency" -> "partnership_agency"
      "Commissioned rehabilitative services (CRS) provider" -> "crs_provider"
      "Someone else (include who in the step)" -> "someone_else"
      else -> "person_on_probation"
    }
  }
}
