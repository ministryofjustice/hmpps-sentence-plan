package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request.CommandResponse

data class GroupCommandResult(
  val commands: List<CommandResponse>,
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
