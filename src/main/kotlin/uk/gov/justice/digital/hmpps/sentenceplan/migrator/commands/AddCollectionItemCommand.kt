package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: UUID,
  val answers: Map<String, Value>,
  val properties: Map<String, Value>,
  val index: Int?,
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
