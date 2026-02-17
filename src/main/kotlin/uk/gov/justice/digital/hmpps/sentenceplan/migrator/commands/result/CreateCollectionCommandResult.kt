package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result

import java.util.UUID

data class CreateCollectionCommandResult(
  val collectionUuid: UUID,
) : CommandResult {
  override val message = "Collection created successfully with UUID $collectionUuid"
  override val success = true
}
