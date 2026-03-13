package uk.gov.justice.digital.hmpps.sentenceplan.migrator.runner

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.PlanMigrator
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.Stats

@Component
class MigrationRunner(
  private val planRepository: PlanRepository,
  private val planMigrator: PlanMigrator,
) {
  fun run(planIds: List<Long>?) {
    log.info("Starting migration")
    log.info("Migrating plans: ${planIds?.joinToString() ?: "All"}")

    Stats.start()

    val failedPlans: MutableMap<Long, String> = mutableMapOf()

    val pageSize = 50
    var hasNext = true
    var totalPages: Int? = null
    var pageNumber = 0
    while (hasNext) {
      val pageRequest = PageRequest.of(0, pageSize)
      val page = planIds
        ?.let { planRepository.findAllByIdInAndMigratedFalse(planIds, pageRequest) }
        ?: planRepository.findAllByMigratedFalseAndIdNotIn(failedPlans.keys, pageRequest)

      if (totalPages == null) {
        totalPages = page.totalPages
      }
      hasNext = page.hasNext()
      if (!page.hasContent()) break

      log.info("Migrating batch of ${page.content.size} items in page ${pageNumber++} of $totalPages")
      page.content.forEach { plan ->
        try {
          planMigrator.migrate(plan)
        } catch (e: Exception) {
          log.warn("Failed to migrate ${plan.id}: ${e.stackTraceToString()}")
          failedPlans[plan.id!!] = e.message ?: "Unknown error"
        }
      }
    }
    log.info("Finished migration in ${Stats.getDuration().toMinutes()} minutes")
    log.info("Migrated ${Stats.numberOfPlans} plans totalling ${Stats.numberOfVersions} versions and created ${Stats.numberOfEvents} events")
    log.info("Failed to migrate ${failedPlans.size} plans")
    failedPlans.forEach { (planId, message) -> log.error("Failed to migrate plan $planId: $message") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
