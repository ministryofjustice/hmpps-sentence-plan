package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class AddCollectionItemCommand(
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val properties: Map<String, List<String>>,
  val index: Int?,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionItemUuid: UUID = UUID.randomUUID()
}
