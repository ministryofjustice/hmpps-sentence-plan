package uk.gov.justice.digital.hmpps.sentenceplan.data

import java.time.LocalDate

data class CaseDetail(
  var name: Name? = Name(),
  var crn: String? = null,
  var tier: String? = null,
  var dateOfBirth: String? = null,
  var nomisId: String? = null,
  var region: String? = null,
  var keyWorker: KeyWorker? = KeyWorker(),
  var inCustody: Boolean? = null,
  val sentences: List<Sentence> = emptyList(),
)

data class KeyWorker(
  var name: Name? = Name(),
  var unallocated: Boolean? = null,
)

data class Name(
  var forename: String? = null,
  var middleName: String? = null,
  var surname: String? = null,
)

data class PopInfoResponse(
  var title: String? = null,
  var firstName: String? = null,
  var lastName: String? = null,
  var gender: String? = null,
  var doB: String? = null,
  var crn: String? = null,
  var prc: String? = null,
  var courtOrderRequirements: Map<String, Any>? = null,
  val sentences: List<Sentence> = emptyList(),
)

data class Sentence(
  val description: String?,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val programmeRequirement: Boolean = false,
  val unpaidWorkHoursOrdered: Int = 0,
  val unpaidWorkMinutesCompleted: Int = 0,
  val rarDaysOrdered: Int = 0,
  val rarDaysCompleted: Int = 0,
  val rarRequirement: Boolean,
)
