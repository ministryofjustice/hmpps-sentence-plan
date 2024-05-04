package uk.gov.justice.digital.hmpps.sentenceplan.data

data class RiskResponse(
  val overallRisk: ScoreEnum,
  val assessedOn: String,
  val riskInCommunity: RiskInCommunityResponse,
  val riskInCustody: RiskInCustodyResponse,
)

data class RiskInCommunityResponse(
  val public: ScoreEnum,
  val children: ScoreEnum,
  val knownAdult: ScoreEnum,
  val staff: ScoreEnum,
)

data class RiskInCustodyResponse(
  val public: ScoreEnum,
  val children: ScoreEnum,
  val knownAdult: ScoreEnum,
  val staff: ScoreEnum,
  val prisoners: ScoreEnum,
)
