package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class CreateCollectionCommand(
  val name: String,
  val parentCollectionItemUuid: UUID?,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  @JsonIgnore
  val collectionUuid: UUID = UUID.randomUUID()
}
