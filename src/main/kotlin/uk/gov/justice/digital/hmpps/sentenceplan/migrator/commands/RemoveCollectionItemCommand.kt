package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import java.util.UUID

data class RemoveCollectionItemCommand(
  val collectionItemUuid: UUID,
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
