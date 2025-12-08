package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.CountersigningStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.time.LocalDateTime
import java.util.UUID

typealias PlanVersionsResponse = List<PlanVersionDetails>

data class PlanVersionDetails(
  val uuid: UUID,
  val version: Int,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val status: CountersigningStatus,
) {
  companion object {
    private fun from(planVersion: PlanVersionEntity): PlanVersionDetails = with(planVersion) {
      PlanVersionDetails(
        uuid,
        version,
        createdDate,
        updatedDate,
        status,
      )
    }

    fun fromAll(planVersions: List<PlanVersionEntity>): List<PlanVersionDetails> = planVersions.map(::from)
  }
}
