package uk.gov.justice.digital.hmpps.sentenceplan.migrator.runner

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.Stats
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.PlanMigrator
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.GroupCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.Requestable

fun List<Requestable>.getCommandCount(): Int = sumOf { command ->
  val added = when (command) {
    is GroupCommand -> 1 + command.commands.getCommandCount()
    else -> 1
  }
  added
}

@Component
class MigrationRunner(
  private val planRepository: PlanRepository,
  private val migrator: PlanMigrator,
) {
  fun run(planId: Long) {
    planRepository.findByIdAndMigratedFalse(planId).run(migrator::run)
  }

  fun run() {
    log.info("Starting migration")

    Stats.start()

    val pageSize = 50
    var hasNext = true
    var totalPages: Int? = null
    var pageNumber = 0
    while (hasNext) {
      val pageRequest = PageRequest.of(0, pageSize)
      val page = planRepository.findAllByMigratedFalse(pageRequest)

      if (totalPages == null) {
        totalPages = page.totalPages
      }
      if (!page.hasContent()) break

      log.info("Migrating batch of ${page.content.size} items in page ${pageNumber++} of $totalPages")
      page.content.forEach { plan -> migrator.run(plan) }

      hasNext = page.hasNext()
    }
    log.info("Finished migration in ${Stats.getDuration().toMinutes()} minutes")
    log.info("Migrated ${Stats.numberOfVersions} versions and created ${Stats.numberOfEvents} events")
  }


  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
