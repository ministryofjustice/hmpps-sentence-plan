package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Command
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CommandResult

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)
