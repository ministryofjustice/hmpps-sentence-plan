package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AddCollectionItemCommandResult::class, name = "AddCollectionItemCommandResult"),
  JsonSubTypes.Type(value = CommandSuccessCommandResult::class, name = "CommandSuccessCommandResult"),
  JsonSubTypes.Type(value = CreateAssessmentCommandResult::class, name = "CreateAssessmentCommandResult"),
  JsonSubTypes.Type(value = CreateCollectionCommandResult::class, name = "CreateCollectionCommandResult"),
  JsonSubTypes.Type(value = GroupCommandResult::class, name = "GroupCommandResult"),
)
sealed interface CommandResult {
  val success: Boolean
  val message: String
}
