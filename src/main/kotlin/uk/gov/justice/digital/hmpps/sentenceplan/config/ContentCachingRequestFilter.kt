package uk.gov.justice.digital.hmpps.sentenceplan.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException

/**
 * This class allows us to get the request body in JpaAuditConfig when the user who we need to attribute a change
 * to is in the request body rather than the header. Normally a request body can only be accessed once, this caches
 * the content of the body for later use.
 */
class ContentCachingRequestFilter : HttpFilter() {

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
    val wrappedRequest = ContentCachingRequestWrapper(request)
    chain.doFilter(wrappedRequest, response)
  }
}
