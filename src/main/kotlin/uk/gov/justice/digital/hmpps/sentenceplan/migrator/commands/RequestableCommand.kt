package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: User
  val assessmentUuid: UUID
}
