package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result

data class TestableCommandResult(
  override val message: String,
  override val success: Boolean = true,
) : CommandResult
