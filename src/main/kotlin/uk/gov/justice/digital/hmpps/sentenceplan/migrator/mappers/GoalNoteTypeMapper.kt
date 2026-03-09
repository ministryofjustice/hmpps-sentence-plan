package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteType

class GoalNoteTypeMapper {
  companion object {
    fun map(type: GoalNoteType) = when (type) {
      GoalNoteType.REMOVED -> "REMOVED"
      GoalNoteType.ACHIEVED -> "ACHIEVED"
      GoalNoteType.READDED -> "READDED"
      // TODO: Confirm how we map these values
      GoalNoteType.PROGRESS -> "PROGRESS"
    }
  }
}