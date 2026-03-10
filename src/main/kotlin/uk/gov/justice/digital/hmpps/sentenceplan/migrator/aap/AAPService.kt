package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.Stats
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Requestable
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.getCommandCount
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request.CommandsRequest
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request.CommandsResponse
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CommandResult
import java.time.LocalDateTime
import java.util.UUID

@Service
class AAPService(
    @param:Qualifier("assessmentPlatformClient")
  private val assessmentPlatformClient: WebClient,
) {
  final inline fun <reified T : CommandResult> dispatchCommand(timestamp: LocalDateTime, command: Requestable) =
    dispatchCommands(timestamp, listOf(command)).extractSingle<T>()

  fun dispatchCommands(timestamp: LocalDateTime, commands: List<Requestable>): CommandsResponse {
    val requestCommandCount = commands.getCommandCount()
    Stats.numberOfEvents += requestCommandCount

    return assessmentPlatformClient
      .post()
      .uri { uriBuilder -> uriBuilder.path("/command").queryParam("backdateTo", timestamp.toString()).build() }
      .bodyValue(CommandsRequest(commands))
      .retrieve()
      .bodyToMono(CommandsResponse::class.java)
      .block()
      ?: throw RuntimeException("Empty response from Assessment Platform API")
  }

  fun deleteAssessment(assessmentUuid: UUID) = assessmentPlatformClient
    .delete()
    .uri("/assessment/$assessmentUuid")
    .retrieve()
    .toBodilessEntity()
    .block()
}
