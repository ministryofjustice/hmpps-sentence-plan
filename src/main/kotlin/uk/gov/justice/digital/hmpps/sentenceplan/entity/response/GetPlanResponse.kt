package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import java.util.UUID

enum class PlanState {
  COMPLETE,
  INCOMPLETE,
}

data class GetPlanResponse(
  var sentencePlanId: UUID,
  var sentencePlanVersion: Long,
  var planComplete: PlanState,
  var planType: PlanType,
  var lastUpdatedTimestampSP: Long,
)