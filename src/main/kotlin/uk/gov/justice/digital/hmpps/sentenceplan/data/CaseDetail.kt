package uk.gov.justice.digital.hmpps.sentenceplan.data

data class CaseDetail(
  var name: Name? = Name(),
  var crn: String? = null,
  var tier: String? = null,
  var dateOfBirth: String? = null,
  var nomisId: String? = null,
  var region: String? = null,
  var keyWorker: KeyWorker? = KeyWorker(),
  var inCustody: Boolean? = null,
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
)
