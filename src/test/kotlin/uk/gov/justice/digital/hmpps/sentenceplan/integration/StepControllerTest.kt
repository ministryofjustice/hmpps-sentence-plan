package uk.gov.justice.digital.hmpps.sentenceplan.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS
import java.util.UUID

private const val TEST_DATA_STEP_UUID = "71793b64-545e-4ae7-9936-610639093857"

@DisplayName("Step Controller Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = [ "/db/test/plan_data.sql", "/db/test/goals_data.sql", "/db/test/step_data.sql" ], executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = [ "/db/test/step_cleanup.sql", "/db/test/goals_cleanup.sql", "/db/test/plan_cleanup.sql" ], executionPhase = AFTER_TEST_CLASS)
class StepControllerTest : IntegrationTestBase() {

  @Test
  fun `get step by existing UUID should return OK`() {
    webTestClient.get().uri("/steps/${TEST_DATA_STEP_UUID}")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_SENTENCE_PLAN_READ")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `get step by non-existent UUID should return not found`() {
    val randomStepUuid = UUID.randomUUID()
    webTestClient.get().uri("/steps/$randomStepUuid")
      .headers(setAuthorisation(user = "Tom C", roles = listOf("ROLE_SENTENCE_PLAN_READ")))
      .exchange()
      .expectStatus().isNotFound
  }
}
