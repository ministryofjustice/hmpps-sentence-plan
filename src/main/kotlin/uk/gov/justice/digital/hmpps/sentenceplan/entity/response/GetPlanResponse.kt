package uk.gov.justice.digital.hmpps.sentenceplan.entity.response

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanAgreementStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import java.time.LocalDateTime
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
  var lastUpdatedTimestampSP: LocalDateTime,
) {
  companion object {
    fun from(planVersionEntity: PlanVersionEntity): GetPlanResponse {
      val planComplete = if (planVersionEntity.agreementStatus == PlanAgreementStatus.DRAFT) {
        PlanState.INCOMPLETE
      } else {
        PlanState.COMPLETE
      }

      return GetPlanResponse(
        sentencePlanId = planVersionEntity.plan!!.uuid,
        sentencePlanVersion = planVersionEntity.version.toLong(),
        lastUpdatedTimestampSP = planVersionEntity.updatedDate,
        planComplete = planComplete,
        planType = planVersionEntity.planType,
      )
    }
  }
}
