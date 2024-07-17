package uk.gov.justice.digital.hmpps.sentenceplan.data

data class Goal(
  val title: String,
  val areaOfNeed: String,
  val targetDate: String? = null,
  val relatedAreasOfNeed: List<String> = emptyList(),
)
