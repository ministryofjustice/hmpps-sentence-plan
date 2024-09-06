package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class AuditorAwareImpl : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> {
    return Optional.of(SecurityContextHolder.getContext().authentication.getName())
  }
}
