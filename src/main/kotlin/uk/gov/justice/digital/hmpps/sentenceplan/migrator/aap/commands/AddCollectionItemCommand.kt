package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value

class AddCollectionItemCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val collection: CreateCollectionCommand? = null,
  var collectionUuid: String? = null,
  val answers: Map<String, Value>,
  val properties: Map<String, Value>,
  val index: Int?,
  val assessmentUuid: String,
) : Requestable,
  Resolvable {
  override fun resolve(
    commands: List<Requestable>,
  ): Requestable {
    if (collection !== null && collectionUuid.isNullOrEmpty()) {
      collectionUuid = commands.indexOfFirst { it === collection }
        .also { require(it >= 0) { "Collection not found" } }
        .let { index -> "@$index" }
    }

    return this
  }
}
