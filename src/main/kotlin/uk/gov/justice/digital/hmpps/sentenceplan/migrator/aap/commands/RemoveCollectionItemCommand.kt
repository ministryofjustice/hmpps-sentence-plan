package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

class RemoveCollectionItemCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val assessmentUuid: String,
  val collectionItem: AddCollectionItemCommand? = null,
  var collectionItemUuid: String? = null,
) : Requestable,
  Resolvable {
  override fun resolve(
    commands: List<Requestable>,
  ): Requestable {
    if (collectionItem !== null && collectionItemUuid.isNullOrEmpty()) {
      collectionItemUuid = commands.indexOfFirst { it === collectionItem }
        .also { require(it >= 0) { "Collection not found" } }
        .let { index -> "@$index" }
    }

    return this
  }
}
