package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${delius-api.base-url}") val deliusApiBaseUrl: String,
  @Value("\${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun deliusRestClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "delius-api", url = deliusApiBaseUrl, timeout)
}
