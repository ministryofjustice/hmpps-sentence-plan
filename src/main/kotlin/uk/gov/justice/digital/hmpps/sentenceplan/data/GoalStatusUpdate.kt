package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus

class GoalStatusUpdate(
  val status: GoalStatus,
  val note: String,
)
