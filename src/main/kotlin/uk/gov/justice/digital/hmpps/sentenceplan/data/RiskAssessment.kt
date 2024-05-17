package uk.gov.justice.digital.hmpps.sentenceplan.data

data class RiskAssessment(
  val riskToSelf: RiskToSelf,
  val otherRisks: OtherRisks,
  val summary: Summary,
  val assessedOn: String,
) {
  data class RiskToSelf(
    val suicide: RiskDetail,
    val selfHarm: RiskDetail,
    val custody: RiskDetail,
    val hostelSetting: RiskDetail,
    val vulnerability: RiskDetail,
  ) {
    data class RiskDetail(
      var risk: String? = null,
      var previous: String? = null,
      var previousConcernsText: String? = null,
      var current: String? = null,
      var currentConcernsText: String? = null,
    )
  }

  data class OtherRisks(
    val escapeOrAbscond: String,
    val controlIssuesDisruptiveBehaviour: String,
    val breachOfTrust: String,
    val riskToOtherPrisoners: String,
  )

  data class Summary(
    val whoIsAtRisk: String,
    val natureOfRisk: String,
    val riskImminence: String,
    val riskIncreaseFactors: String,
    val riskMitigationFactors: String,
    val riskInCommunity: Map<String, List<String>>,
    val riskInCustody: Map<String, List<String>>,
    val overallRiskLevel: String,
  )
}
