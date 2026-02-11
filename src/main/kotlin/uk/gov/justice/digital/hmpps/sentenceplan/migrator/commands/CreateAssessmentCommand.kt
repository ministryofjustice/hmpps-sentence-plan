package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value
import java.util.UUID

data class CreateAssessmentCommand(
  override val user: UserDetails,
  val formVersion: String,
  val assessmentType: String,
  val identifiers: Map<IdentifierType, String>? = null,
  val properties: Map<String, Value>? = null,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: UUID = UUID.randomUUID()
}
