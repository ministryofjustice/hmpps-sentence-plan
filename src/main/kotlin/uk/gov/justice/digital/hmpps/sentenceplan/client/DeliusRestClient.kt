package uk.gov.justice.digital.hmpps.sentenceplan.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.sentenceplan.config.DeliusApiOauth2Config
import uk.gov.justice.digital.hmpps.sentenceplan.data.CaseDetail

@FeignClient(
  name = "delius-api",
  url = "\${delius-api.base-url}",
  configuration = [DeliusApiOauth2Config::class],
)
interface DeliusRestClient {

  @GetMapping("/case-details/{crn}")
  fun getCaseDetail(@PathVariable("crn") crn: String): CaseDetail?
}
