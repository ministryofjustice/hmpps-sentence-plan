package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.Value
import java.util.UUID

data class UpdateCollectionItemAnswersCommand(
  val collectionItemUuid: UUID,
  val added: Map<String, Value>,
  val removed: List<String>,
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  override val timeline: Timeline? = null,
) : RequestableCommand
