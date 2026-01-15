package uk.gov.justice.digital.hmpps.sentenceplan.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.util.ContentCachingRequestWrapper
import uk.gov.justice.digital.hmpps.sentenceplan.data.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PractitionerRepository

class JpaAuditConfigTest {

  private val practitionerRepository: PractitionerRepository = mockk()
  private val jpaAuditConfig = JpaAuditConfig(practitionerRepository)

  @Test
  fun `should return PractitionerEntity when UserDetails present in request`() {
    val request: ContentCachingRequestWrapper = mockk()
    val requestAttributes: ServletRequestAttributes = mockk()
    every { requestAttributes.request } returns (request)
    RequestContextHolder.setRequestAttributes(requestAttributes)

    val authentication: Authentication = mockk()
    every { authentication.name } returns ("SYSTEM|systemUser")
    val securityContext: SecurityContext = mockk()
    every { securityContext.authentication }.returns(authentication)
    SecurityContextHolder.setContext(securityContext)

    val userDetails = UserDetails("1234", "testUser")
    every { request.contentAsString } returns (ObjectMapper().writeValueAsString(mapOf("userDetails" to userDetails)))

    val practitionerEntity = PractitionerEntity(externalId = "1234", username = "testUser")
    val systemPractitionerEntity = PractitionerEntity(externalId = "SYSTEM", username = "systemUser")
    every { practitionerRepository.findByExternalId("1234") } returns (practitionerEntity)
    every { practitionerRepository.findByExternalId("SYSTEM") } returns (systemPractitionerEntity)

    val auditor = jpaAuditConfig.auditorProvider().currentAuditor
    assertThat(practitionerEntity).isEqualTo(auditor.get())
  }

  @Test
  fun `should return System PractitionerEntity when no UserDetails in request`() {
    val requestAttributes: ServletRequestAttributes = mockk()
    every { requestAttributes.request } returns (mockk())
    RequestContextHolder.setRequestAttributes(requestAttributes)

    val authentication: Authentication = mockk()
    every { authentication.name } returns ("SYSTEM|systemUser")
    val securityContext: SecurityContext = mockk()
    every { securityContext.authentication } returns (authentication)
    SecurityContextHolder.setContext(securityContext)

    val practitionerEntity = PractitionerEntity(externalId = "SYSTEM", username = "systemUser")
    every { practitionerRepository.findByExternalId("SYSTEM") } returns (practitionerEntity)

    val auditor = jpaAuditConfig.auditorProvider().currentAuditor
    assertEquals(practitionerEntity, auditor.get())
  }

  @Test
  fun `should create new PractitionerEntity when Practitioner not found`() {
    val requestAttributes: ServletRequestAttributes = mockk()
    every { requestAttributes.request } returns (mockk())
    RequestContextHolder.setRequestAttributes(requestAttributes)

    val authentication: Authentication = mockk()
    every { authentication.name } returns ("SYSTEM|systemUser")

    val securityContext: SecurityContext = mockk()
    every { securityContext.authentication } returns (authentication)
    SecurityContextHolder.setContext(securityContext)

    every { practitionerRepository.findByExternalId("SYSTEM") }.throws(EmptyResultDataAccessException(1))
    val newPractitionerEntity = PractitionerEntity(externalId = "SYSTEM", username = "systemUser")
    every { practitionerRepository.save(any()) } returns (newPractitionerEntity)

    val auditor = jpaAuditConfig.auditorProvider().currentAuditor
    assertEquals(newPractitionerEntity, auditor.get())
  }
}
