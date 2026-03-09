package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CommandResult

data class CommandsResponse(
  val commands: List<CommandResponse>,
) {
  inline fun <reified T : CommandResult> extractNthInstance(index: Int): T = commands.map { it.result }
    .filterIsInstance<T>().getOrNull(index)
    ?: error("No response found at index $index for ${T::class.simpleName}")

  inline fun <reified T : CommandResult> extractSingle(): T = commands.map { it.result }
    .filterIsInstance<T>()
    .singleOrNull()
    ?: error("Expected exactly one ${T::class.simpleName}")
}
