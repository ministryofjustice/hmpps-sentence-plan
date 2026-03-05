package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.time.LocalDateTime

class RollbackCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val assessmentUuid: String,
  val pointInTime: LocalDateTime,
) : Requestable
