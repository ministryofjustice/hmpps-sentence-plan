package uk.gov.justice.digital.hmpps.sentenceplan.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.sentenceplan.config.ARNSApiOauth2Config
import uk.gov.justice.digital.hmpps.sentenceplan.data.RiskAssessment

@FeignClient(
  name = "arns-api",
  url = "\${arns-api.base-url}",
  configuration = [ARNSApiOauth2Config::class],
)
interface ARNSRestClient {

  @GetMapping("/risks/crn/{crn}")
  fun getRiskScoreInfoByCrn(@PathVariable("crn") crn: String): RiskAssessment?
}
