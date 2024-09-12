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
      var practitionerEntity: PractitionerEntity

      if (SecurityContextHolder.getContext().authentication != null) {
        val usernameParts = SecurityContextHolder.getContext().authentication.name.split('|')
        val uuid = usernameParts[0]
        val username = usernameParts[1]

        try {
          practitionerEntity = practitionerRepository.findByUsername(username)
        } catch (e: Exception) {
          val practitioner = PractitionerEntity(
            uuid = uuid,
            username = username,
          )
          practitionerEntity = practitionerRepository.save(practitioner)
        }
      } else {
        practitionerEntity = practitionerRepository.findByUsername(usernameAuthenticationNotAvailable)
      }

      Optional.of(practitionerEntity)
    }
  }
}
