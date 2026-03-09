package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value

class UpdateAssessmentPropertiesCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val assessmentUuid: String,
  val added: Map<String, Value>,
  val removed: List<String>,
) : Requestable
