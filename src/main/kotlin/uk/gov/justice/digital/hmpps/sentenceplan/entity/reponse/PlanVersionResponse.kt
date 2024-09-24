package uk.gov.justice.digital.hmpps.sentenceplan.entity.reponse

import java.util.UUID

data class PlanVersionResponse(
  val planUuid: UUID,
  val planVersion: Long,
)
