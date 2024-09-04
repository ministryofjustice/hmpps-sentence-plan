package uk.gov.justice.digital.hmpps.sentenceplan.stub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.sentenceplan.data.CaseDetail
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment

class StubData {
  companion object {
    val objectMapper = jacksonObjectMapper()
    fun getRiskScoreInfoByCrn(crn: String): RiskAssessment {
      val jsonString = """{
             "riskToSelf":{
                "suicide":{
                   "risk":"NO"
                },
                "selfHarm":{
                   "risk":"YES",
                   "previous":"YES",
                   "previousConcernsText":"N/A",
                   "current":"YES",
                   "currentConcernsText":"N/A"
                },
                "custody":{
                   "risk":"YES",
                   "previous":"NO",
                   "current":"YES",
                   "currentConcernsText":"N/A"
                },
                "hostelSetting":{
                   "risk":"YES",
                   "previous":"NO",
                   "current":"YES",
                   "currentConcernsText":"N/A"
                },
                "vulnerability":{
                   "risk":"YES"
                }
             },
             "otherRisks":{
                "escapeOrAbscond":"YES",
                "controlIssuesDisruptiveBehaviour":"YES",
                "breachOfTrust":"YES",
                "riskToOtherPrisoners":"NO"
             },
             "summary":{
                "whoIsAtRisk":"People around him",
                "natureOfRisk":"People around him",
                "riskImminence":"When with others",
                "riskIncreaseFactors":"Access to Alcohol",
                "riskMitigationFactors":"Being alone",
                "riskInCommunity":{
                   "LOW":[
                      "Children"
                   ],
                   "HIGH":[
                      "Public",
                      "Known Adult",
                      "Staff"
                   ]
                },
                "riskInCustody":{
                   "LOW":[
                      "Children"
                   ],
                   "HIGH":[
                      "Public",
                      "Known Adult",
                      "Staff",
                      "Prisoners"
                   ]
                },
                "overallRiskLevel":"HIGH"
             },
             "assessedOn":"2024-04-11T14:29:00"
          }
      """
      return objectMapper.readValue<RiskAssessment>(jsonString)
    }

    fun getCaseDetail(crn: String): CaseDetail {
      val jsonString = """{
       "name": {
        "forename": "Joan",
        "middleName": "",
        "surname": "Scott"
       },
       "crn": "12345678",
       "tier": "tier",
       "dateOfBirth": "01/01/1997",
       "nomisId": "some_id",
       "region": "region",
       "keyWorker" : {
         "name": {
          "forename": "abc",
          "middleName": "xyz",
          "surname": "pqr"
         },
         "unallocated": true
       },
       "inCustody": false
      }"""
      return objectMapper.readValue<CaseDetail>(jsonString)
    }
  }
}
