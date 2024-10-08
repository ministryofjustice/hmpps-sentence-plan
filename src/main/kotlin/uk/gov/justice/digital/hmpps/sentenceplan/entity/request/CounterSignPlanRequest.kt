package uk.gov.justice.digital.hmpps.sentenceplan.entity.request

data class CounterSignPlanRequest(
  val signType: CountersignType,
  val sentencePlanVersion: Long,
)

enum class CountersignType {
  COUNTERSIGNED,
  AWAITING_DOUBLE_COUNTERSIGN,
  DOUBLE_COUNTERSIGNED,
  REJECTED,
}
