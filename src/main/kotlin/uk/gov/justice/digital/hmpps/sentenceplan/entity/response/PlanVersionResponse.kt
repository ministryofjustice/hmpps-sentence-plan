package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import java.util.UUID

data class PlanVersionResponse(
  val planUuid: UUID,
  val planVersion: Long,
) {
  companion object {
    fun from(planEntity: PlanEntity): PlanVersionResponse {
      return PlanVersionResponse(
        planUuid = planEntity.uuid,
        // Set proper version later
        planVersion = planEntity.currentVersion.version,
      )
    }
  }
}
