package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.util.UUID

data class UpdateFormVersionCommand(
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  val version: String,
  override val timeline: Timeline? = null,
) : RequestableCommand
