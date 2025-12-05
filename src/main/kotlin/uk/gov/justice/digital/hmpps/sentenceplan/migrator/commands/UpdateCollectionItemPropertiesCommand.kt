package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class UpdateCollectionItemPropertiesCommand(
  val collectionItemUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
