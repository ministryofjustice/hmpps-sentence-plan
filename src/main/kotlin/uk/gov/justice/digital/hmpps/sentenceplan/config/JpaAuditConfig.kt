package uk.gov.justice.digital.hmpps.sentenceplan.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.ContentCachingRequestWrapper
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerRepository
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditConfig(private val practitionerRepository: PractitionerRepository) {

  @Bean
  fun auditorProvider(): AuditorAware<PractitionerEntity> = AuditorAware<PractitionerEntity> {
    val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
    val request = requestAttributes?.request

    var externalId = ""
    var username = ""
    var practitionerEntity: PractitionerEntity

    if (request != null && request is ContentCachingRequestWrapper) {
      val userDetails = request.let { readUserDetailsFromRequest(it) }

      if (userDetails != null && userDetails.id.isNotEmpty() && userDetails.name.isNotEmpty()) {
        externalId = userDetails.id
        username = userDetails.name
      }
    }

    if (externalId.isEmpty()) {
      val authenticationName = SecurityContextHolder.getContext().authentication.name

      val usernameParts = authenticationName.split('|')

      if (usernameParts.size != 2) {
        externalId = "SYSTEM"
        username = usernameParts[0]
      } else {
        externalId = usernameParts[0]
        username = usernameParts[1]
      }
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

  fun readUserDetailsFromRequest(request: ContentCachingRequestWrapper): UserDetails? {
    val requestBody = request.contentAsString.let {
      ObjectMapper().readTree(it)
    }

    val userDetailsNode = requestBody.get("userDetails")

    return userDetailsNode?.let {
      UserDetails(
        id = it.get("id").asText(),
        name = it.get("name").asText(),
      )
    }
  }
}
