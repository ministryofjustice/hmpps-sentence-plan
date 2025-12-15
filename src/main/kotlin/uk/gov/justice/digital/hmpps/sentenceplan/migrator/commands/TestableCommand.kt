package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

data class TestableCommand(
  override val timeline: Timeline? = null,
  val param: String = "",
) : Command
