package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result

data class CommandSuccessCommandResult(
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
