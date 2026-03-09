package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

class UpdateFormVersionCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val assessmentUuid: String,
  val version: String,
) : Requestable
