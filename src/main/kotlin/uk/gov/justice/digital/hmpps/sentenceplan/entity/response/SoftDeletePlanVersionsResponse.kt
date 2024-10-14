package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.util.UUID

data class SoftDeletePlanVersionsResponse(
  val planId: UUID,
  val planVersion: Long,
  val createdFromVersion: Long? = null,
  val versionsSoftDeleted: List<Int>? = null,
  val versionsRestored: List<Int>? = null,
) {
  companion object {
    fun from(
      planVersion: PlanVersionEntity,
      planId: UUID,
      softDelete: Boolean,
      updatedVersions: List<Int>,
      createdFromVersion: Long? = null,
    ): SoftDeletePlanVersionsResponse {
      return SoftDeletePlanVersionsResponse(
        planId = planId,
        planVersion = planVersion.version.toLong(),
        createdFromVersion = createdFromVersion,
        versionsSoftDeleted = if (softDelete) updatedVersions else null,
        versionsRestored = if (!softDelete) updatedVersions else null,
      )
    }
  }
}
