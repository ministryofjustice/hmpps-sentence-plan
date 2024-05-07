package uk.gov.justice.digital.hmpps.sentenceplan.stub

import com.google.gson.Gson
import uk.gov.justice.digital.hmpps.sentenceplan.data.CaseDetail
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment

class StubData {
  companion object {
    fun getRiskScoreInfoByCrn(crn: String): RiskAssessment {
      val jsonString = """{
          "riskToSelf": {
            "suicide": {
              "risk": "Yes, No, Don't know, null",
              "previous": "Yes, No, Don't know",
              "previousConcernsText": "Risk of self harms concerns due to ...",
              "current": "Yes, No, Don't know",
              "currentConcernsText": "Risk of self harms concerns due to ..."
            },
            "selfHarm": {
              "risk": "Yes, No, Don't know, null",
              "previous": "Yes, No, Don't know",
              "previousConcernsText": "Risk of self harms concerns due to ...",
              "current": "Yes, No, Don't know",
              "currentConcernsText": "Risk of self harms concerns due to ..."
            },
            "custody": {
              "risk": "Yes, No, Don't know, null",
              "previous": "Yes, No, Don't know",
              "previousConcernsText": "Risk of self harms concerns due to ...",
              "current": "Yes, No, Don't know",
              "currentConcernsText": "Risk of self harms concerns due to ..."
            },
            "hostelSetting": {
              "risk": "Yes, No, Don't know, null",
              "previous": "Yes, No, Don't know",
              "previousConcernsText": "Risk of self harms concerns due to ...",
              "current": "Yes, No, Don't know",
              "currentConcernsText": "Risk of self harms concerns due to ..."
            },
            "vulnerability": {
              "risk": "Yes, No, Don't know, null",
              "previous": "Yes, No, Don't know",
              "previousConcernsText": "Risk of self harms concerns due to ...",
              "current": "Yes, No, Don't know",
              "currentConcernsText": "Risk of self harms concerns due to ..."
            }
          },
          "otherRisks": {
            "escapeOrAbscond": "YES",
            "controlIssuesDisruptiveBehaviour": "YES",
            "breachOfTrust": "YES",
            "riskToOtherPrisoners": "YES"
          },
          "summary": {
            "whoIsAtRisk": "X, Y and Z are at risk",
            "natureOfRisk": "The nature of the risk is X",
            "riskImminence": "the risk is imminent and more probably in X situation",
            "riskIncreaseFactors": "If offender in situation X the risk can be higher",
            "riskMitigationFactors": "Giving offender therapy in X will reduce the risk",
            "riskInCommunity": {
              "HIGH": [
                "Children",
                "Public",
                "Know adult"
              ],
              "MEDIUM": [
                "Staff"
              ],
              "LOW": [
                "Prisoners"
              ]
            },
            "riskInCustody": {
              "HIGH": [
                "Know adult"
              ],
              "VERY_HIGH": [
                "Staff",
                "Prisoners"
              ],
              "LOW": [
                "Children",
                "Public"
              ]
            },
            "overallRiskLevel": "HIGH"
          },
          "assessedOn": "2024-05-02T10:31:34"
        }
      """
      var gson = Gson()
      var riskAssessment = gson.fromJson(jsonString, RiskAssessment::class.java)
      return riskAssessment
    }

    fun getCaseDetail(crn: String): CaseDetail {
      val jsonString = """{
       "name": {
        "forename": "abc",
        "middleName": "xyz",
        "surname": "pqr"
       },
       "crn": "crn",
       "tier": "tier",
       "dateOfBirth": "03-1-2000",
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
      var gson = Gson()
      var caseDetail = gson.fromJson(jsonString, CaseDetail::class.java)
      return caseDetail
    }
  }
}
