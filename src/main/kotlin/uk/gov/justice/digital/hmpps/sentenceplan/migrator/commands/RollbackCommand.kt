package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.time.LocalDateTime
import java.util.UUID

data class RollbackCommand(
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  val pointInTime: LocalDateTime,
  override val timeline: Timeline? = null,
) : RequestableCommand
