package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

sealed interface Requestable : Command {
  val user: UserDetails
}
