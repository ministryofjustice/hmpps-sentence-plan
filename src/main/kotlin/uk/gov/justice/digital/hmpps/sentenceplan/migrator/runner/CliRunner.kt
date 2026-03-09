package uk.gov.justice.digital.hmpps.sentenceplan.migrator.runner

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("migration")
class CliRunner(
  private val migrator: MigrationRunner,
) : CommandLineRunner {
  override fun run(vararg args: String) {
    migrator.run()
  }
}
