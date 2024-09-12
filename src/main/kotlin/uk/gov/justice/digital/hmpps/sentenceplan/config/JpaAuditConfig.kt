package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerRepository
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditConfig(private val practitionerRepository: PractitionerRepository) {

  val usernameAuthenticationNotAvailable = "Not set"

  @Bean
  fun auditorProvider(): AuditorAware<PractitionerEntity> {
    return AuditorAware<PractitionerEntity> {
      if (SecurityContextHolder.getContext().authentication != null) {
        val username = SecurityContextHolder.getContext().authentication.name
        var practitionerEntity: PractitionerEntity
        try {
          practitionerEntity = practitionerRepository.findByUsername(username)
        } catch (e: Exception) {
          val practitioner = PractitionerEntity(
            uuid = "identifier",
            username = SecurityContextHolder.getContext().authentication.name,
          )
          practitionerEntity = practitionerRepository.save(practitioner)
        }
        Optional.of(practitionerEntity)
      } else {
        Optional.of(practitionerRepository.findByUsername(usernameAuthenticationNotAvailable))
      }
    }
  }
}
