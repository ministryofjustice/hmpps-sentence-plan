package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.time.LocalDateTime

class CreateCollectionCommand(
  override val user: UserDetails,
  override val timeline: Timeline? = null,
  var name: String,
  val parentCollectionItem: AddCollectionItemCommand? = null,
  var parentCollectionItemUuid: String? = null,
  val assessmentUuid: String,
  val createdOnTimestamp: LocalDateTime = LocalDateTime.now(),
) : Requestable,
  Resolvable {
  override fun resolve(
    commands: List<Requestable>,
  ): Requestable {
    if (parentCollectionItem !== null && parentCollectionItemUuid.isNullOrEmpty()) {
      parentCollectionItemUuid = commands.indexOfFirst { it === parentCollectionItem }
        .also { require(it >= 0) { "Collection not found" } }
        .let { index -> "@$index" }
    }

    return this
  }
}
