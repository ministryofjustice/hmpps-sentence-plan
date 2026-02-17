package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.CommandResponse

data class GroupCommandResult(
  val commands: List<CommandResponse>,
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
