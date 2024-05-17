package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import java.time.LocalDateTime

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Goal Tests")
class GoalControllerTest : IntegrationTestBase() {

  val currentTime = LocalDateTime.now().toString()

  private val goalRequestBody = GoalEntity(
    title = "abc",
    areaOfNeed = "xzv",
    isAgreed = true,
    agreementNote = "note",
    creationDate = currentTime,
    targetDate = currentTime,
    goalOrder = 1,
  )

  @Test
  fun `create goal should return created`() {
    webTestClient.post().uri("/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_RISK_INTEGRATIONS_RO")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isCreated
  }

  @Test
  fun `create goal should return unauthorized when no auth token`() {
    webTestClient.post().uri("/goals")
      .header("Content-Type", "application/json")
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create goal should return forbidden when no role`() {
    webTestClient.post().uri("/goals")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .bodyValue(goalRequestBody)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `create steps should return unauthorized when no auth token`() {
    webTestClient.post().uri("/goals/1/steps")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create steps should return forbidden when no role`() {
    webTestClient.post().uri("/goals/1/steps")
      .header("Content-Type", "application/json")
      .headers(setAuthorisation(roles = listOf("abc")))
      .exchange()
      .expectStatus().isForbidden
  }
}
