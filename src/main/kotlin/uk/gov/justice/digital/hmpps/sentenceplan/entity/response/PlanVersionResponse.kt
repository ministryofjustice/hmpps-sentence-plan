package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.util.UUID

data class PlanVersionResponse(
  val planId: UUID,
  val planVersion: Long,
) {
  companion object {
    fun from(planEntity: PlanEntity): PlanVersionResponse {
      return PlanVersionResponse(
        planId = planEntity.uuid,
        planVersion = 0L,
      )
    }

    fun from(planVersionEntity: PlanVersionEntity): PlanVersionResponse {
      return PlanVersionResponse(
        planUuid = planVersionEntity.uuid,
        planVersion = 10L,
      )
    }
  }
}
