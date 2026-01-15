package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfig {

  @Bean
  fun contentCachingRequestFilter() = FilterRegistrationBean(ContentCachingRequestFilter()).apply {
    addUrlPatterns("/coordinator/plan", "/coordinator/plan/*") // Only apply this filter to coordinator requests
    order = 1 // Make sure this filter runs first when applicable
  }
}
