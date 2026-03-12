package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.request

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Requestable

data class CommandsRequest(
  val commands: List<Requestable>,
)
