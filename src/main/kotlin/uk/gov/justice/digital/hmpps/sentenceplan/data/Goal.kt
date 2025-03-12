package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus

data class Goal(
  val title: String? = null,
  val areaOfNeed: String? = null,
  val targetDate: String? = null,
  val relatedAreasOfNeed: List<String> = emptyList(),
  var status: GoalStatus? = null,
  val note: String? = null,
  val steps: List<Step> = emptyList(),
)
