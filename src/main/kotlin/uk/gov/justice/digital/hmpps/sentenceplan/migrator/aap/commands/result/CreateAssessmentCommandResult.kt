package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result

data class CreateAssessmentCommandResult(
  val assessmentUuid: String,
) : CommandResult {
  override val message = "Assessment created successfully with UUID $assessmentUuid"
  override val success = true
}
