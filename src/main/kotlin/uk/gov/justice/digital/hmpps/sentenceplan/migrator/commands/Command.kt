package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class Timeline(
  val type: String,
  val data: Map<String, Any>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AddCollectionItemCommand::class, name = "AddCollectionItemCommand"),
  JsonSubTypes.Type(value = CreateAssessmentCommand::class, name = "CreateAssessmentCommand"),
  JsonSubTypes.Type(value = CreateCollectionCommand::class, name = "CreateCollectionCommand"),
  JsonSubTypes.Type(value = GroupCommand::class, name = "GroupCommand"),
  JsonSubTypes.Type(value = RemoveCollectionItemCommand::class, name = "RemoveCollectionItemCommand"),
  JsonSubTypes.Type(value = ReorderCollectionItemCommand::class, name = "ReorderCollectionItemCommand"),
  JsonSubTypes.Type(value = RollbackCommand::class, name = "RollbackCommand"),
  JsonSubTypes.Type(value = UpdateAssessmentAnswersCommand::class, name = "UpdateAssessmentAnswersCommand"),
  JsonSubTypes.Type(value = UpdateAssessmentPropertiesCommand::class, name = "UpdateAssessmentPropertiesCommand"),
  JsonSubTypes.Type(value = UpdateCollectionItemAnswersCommand::class, name = "UpdateCollectionItemAnswersCommand"),
  JsonSubTypes.Type(value = UpdateCollectionItemPropertiesCommand::class, name = "UpdateCollectionItemPropertiesCommand"),
  JsonSubTypes.Type(value = UpdateFormVersionCommand::class, name = "UpdateFormVersionCommand"),
)
sealed interface Command {
  val timeline: Timeline?
}
