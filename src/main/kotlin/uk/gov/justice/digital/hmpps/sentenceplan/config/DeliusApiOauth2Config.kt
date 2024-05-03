package uk.gov.justice.digital.hmpps.sentenceplan.config
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager

@Configuration
class DeliusApiOauth2Config(authorizedClientManager: OAuth2AuthorizedClientManager) :
  FeignConfig(authorizedClientManager) {

  override fun registrationId() = "delius-api"
}
