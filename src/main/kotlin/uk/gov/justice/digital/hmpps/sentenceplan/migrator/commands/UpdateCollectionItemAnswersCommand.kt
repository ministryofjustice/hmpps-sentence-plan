package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value

data class UpdateCollectionItemAnswersCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  val collectionItem: AddCollectionItemCommand? = null,
  var collectionItemUuid: String? = null,
  val added: Map<String, Value>,
  val removed: List<String>,
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
