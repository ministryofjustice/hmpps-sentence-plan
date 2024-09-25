package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

data class CounterSignPlanRequest(
  val signType: SignType,
  val sentencePlanVersion: Long,
)

enum class SignType {
  COUNTERSIGNED,
  AWAITING_DOUBLE_COUNTERSIGN,
  DOUBLE_COUNTERSIGNED,
  REJECTED,
}
