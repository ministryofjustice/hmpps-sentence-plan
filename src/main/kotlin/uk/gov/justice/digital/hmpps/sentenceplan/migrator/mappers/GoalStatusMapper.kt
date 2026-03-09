package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus

class GoalStatusMapper {
  companion object {
    fun map(status: GoalStatus) = when (status) {
      GoalStatus.ACTIVE -> "ACTIVE"
      GoalStatus.FUTURE -> "FUTURE"
      GoalStatus.ACHIEVED -> "ACHIEVED"
      GoalStatus.REMOVED -> "REMOVED"
    }
  }
}