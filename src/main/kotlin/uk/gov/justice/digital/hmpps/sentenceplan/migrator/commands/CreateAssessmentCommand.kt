package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value

data class CreateAssessmentCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val formVersion: String,
  val assessmentType: String,
  val identifiers: Map<IdentifierType, String>? = null,
  val properties: Map<String, Value>? = null,
) : Requestable
