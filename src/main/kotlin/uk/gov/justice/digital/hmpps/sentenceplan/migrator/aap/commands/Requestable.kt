package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

sealed interface Requestable : Command {
  val user: UserDetails
}

fun List<Requestable>.getCommandCount(): Int = sumOf { command ->
  val added = when (command) {
    is GroupCommand -> 1 + command.commands.getCommandCount()
    else -> 1
  }
  added
}
