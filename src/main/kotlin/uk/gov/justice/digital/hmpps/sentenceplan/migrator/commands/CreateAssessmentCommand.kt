package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class CreateAssessmentCommand(
  override val user: User,
  val formVersion: String,
  val properties: Map<String, List<String>> = emptyMap(),
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  override val assessmentUuid: UUID = UUID.randomUUID()
}
