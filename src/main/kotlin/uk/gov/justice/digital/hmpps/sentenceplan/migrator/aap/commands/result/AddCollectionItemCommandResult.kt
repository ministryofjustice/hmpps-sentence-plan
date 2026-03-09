package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result

data class AddCollectionItemCommandResult(
  val collectionItemUuid: String,
) : CommandResult {
  override val message = "Collection item added successfully with UUID $collectionItemUuid"
  override val success = true
}
