package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

class GroupCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  var commands: List<Requestable>,
  val assessmentUuid: String,
) : Requestable,
  Resolvable {
  override fun resolve(commands: List<Requestable>): Requestable {
    this.commands =
      this.commands.fold(emptyList()) { resolved, command -> resolved + (if (command is Resolvable) command.resolve(resolved) else command) as Requestable }

    return this
  }
}
