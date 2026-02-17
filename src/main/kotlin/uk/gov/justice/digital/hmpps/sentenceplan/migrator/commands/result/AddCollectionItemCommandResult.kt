package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result

import java.util.UUID

data class AddCollectionItemCommandResult(
  val collectionItemUuid: UUID,
) : CommandResult {
  override val message = "Collection item added successfully with UUID $collectionItemUuid"
  override val success = true
}
