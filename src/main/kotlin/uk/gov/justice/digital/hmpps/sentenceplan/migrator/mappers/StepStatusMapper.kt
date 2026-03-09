package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepStatus

class StepStatusMapper {
  companion object {
    fun map(status: StepStatus) = when (status) {
      StepStatus.NOT_STARTED -> "NOT_STARTED"
      StepStatus.IN_PROGRESS -> "IN_PROGRESS"
      StepStatus.COMPLETED -> "COMPLETED"
      StepStatus.CANNOT_BE_DONE_YET -> "CANNOT_BE_DONE_YET"
      StepStatus.NO_LONGER_NEEDED -> "NO_LONGER_NEEDED"
    }
  }
}