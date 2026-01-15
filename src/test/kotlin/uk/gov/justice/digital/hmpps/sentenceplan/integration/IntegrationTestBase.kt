package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  protected lateinit var webTestClient: WebTestClient

  @LocalServerPort
  private var port: Int = 0

  @BeforeEach
  fun beforeEach() {
    webTestClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:$port")
      .build()
  }

  @Autowired
  internal lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    user: String = "sentence-plan",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = user, scope = scopes, roles = roles)
}
