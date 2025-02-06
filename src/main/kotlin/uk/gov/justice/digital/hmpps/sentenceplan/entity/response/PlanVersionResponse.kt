package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.util.UUID

data class PlanVersionResponse(
  val planId: UUID,
  val planVersion: Long,
) {
  companion object {
    fun from(planEntity: PlanEntity): PlanVersionResponse = PlanVersionResponse(
      planId = planEntity.uuid,
      planVersion = planEntity.currentVersion?.version?.toLong()!!,
    )

    fun from(planVersionEntity: PlanVersionEntity): PlanVersionResponse = PlanVersionResponse(
      planId = planVersionEntity.uuid,
      planVersion = planVersionEntity.version.toLong(),
    )
  }
}
