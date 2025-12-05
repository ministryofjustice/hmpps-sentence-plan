package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class GroupCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val commands: List<RequestableCommand>,
  override val timeline: Timeline? = null,
) : RequestableCommand
