package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.result

data class CreateCollectionCommandResult(
  val collectionUuid: String,
) : CommandResult {
  override val message = "Collection created successfully with UUID $collectionUuid"
  override val success = true
}
