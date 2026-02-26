package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails

data class ReorderCollectionItemCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val collectionItem: AddCollectionItemCommand? = null,
  var collectionItemUuid: String? = null,
  val index: Int,
  val assessmentUuid: String,
) : Requestable,
  Resolvable {
  override fun resolve(
    commands: List<Requestable>,
  ) {
    if (collectionItem !== null && collectionItemUuid.isNullOrEmpty()) {
      collectionItemUuid = commands.indexOfFirst { it === collectionItem }
        .also { require(it >= 0) { "Collection not found" } }
        .let { index -> "@$index" }
    }
  }
}
