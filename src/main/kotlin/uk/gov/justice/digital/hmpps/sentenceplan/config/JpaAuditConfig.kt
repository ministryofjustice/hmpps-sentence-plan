package uk.gov.justice.digital.hmpps.sentenceplan.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerRepository
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditConfig(private val practitionerRepository: PractitionerRepository) {

  @Bean
  fun auditorProvider(): AuditorAware<PractitionerEntity> = AuditorAware<PractitionerEntity> {
    var practitionerEntity: PractitionerEntity

    val authenticationName = SecurityContextHolder.getContext().authentication.name

    val usernameParts = authenticationName.split('|')
    val externalId: String
    val username: String

    if (usernameParts.size != 2) {
      externalId = "SYSTEM"
      username = usernameParts[0]
    } else {
      externalId = usernameParts[0]
      username = usernameParts[1]
    }

    try {
      practitionerEntity = practitionerRepository.findByExternalId(externalId)
    } catch (e: EmptyResultDataAccessException) {
      val practitioner = PractitionerEntity(
        externalId = externalId,
        username = username,
      )
      practitionerEntity = practitionerRepository.save(practitioner)
    }

    Optional.of(practitionerEntity)
  }
}
