package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${app.services.hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${delius-api.base-url}") val deliusApiBaseUrl: String,
  @param:Value("\${assessment-platform-api.base-url}") val assessmentPlatformBaseUrl: String,
  @param:Value("\${coordinator-api.base-url}") val coordinatorBaseURL: String,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
) {
  @Bean
  fun webClientBuilder(): WebClient.Builder = WebClient.builder()

  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun deliusRestClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "delius-api", url = deliusApiBaseUrl, timeout)

  @Bean
  fun assessmentPlatformClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "assessment-platform-api",
    url = assessmentPlatformBaseUrl,
    Duration.ofMinutes(30), // TODO: remove this debug
  )

  @Bean
  fun coordinatorClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "coordinator-api",
    url = coordinatorBaseURL,
    timeout,
  )
}
