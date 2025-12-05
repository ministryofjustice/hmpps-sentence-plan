package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User
import java.util.UUID

data class ReorderCollectionItemCommand(
  val collectionItemUuid: UUID,
  val index: Int,
  override val user: User,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
