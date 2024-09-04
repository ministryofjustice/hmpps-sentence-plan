package uk.gov.justice.digital.hmpps.sentenceplan.data

import java.util.UUID

data class GoalOrder(
  var goalUuid: UUID,
  var goalOrder: Int? = null,
)
