package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class AuditorAwareImpl {

  @Bean
  fun auditorProvider(): AuditorAware<String> {
    return AuditorAware { Optional.of(SecurityContextHolder.getContext().authentication.name) }
  }
}
