package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: UserDetails
  val assessmentUuid: UUID
}
