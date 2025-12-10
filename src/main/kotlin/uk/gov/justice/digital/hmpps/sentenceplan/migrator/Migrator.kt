package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands.RequestableCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.User

data class CommandsRequest(
  val commands: List<RequestableCommand>,
)

typealias Requests = List<CommandsRequest>

@Component
@Profile("migration")
class Migrator(
  private val planRepository: PlanRepository
) : CommandLineRunner {
  fun migrate(plan: PlanEntity) {
    plan.currentVersion?.let { currentVersion ->
      val creationCommand = CreateAssessmentCommand(
        user = User.from(plan.createdBy),
        formVersion = "1",
        properties = buildMap {
          put("STATUS", listOf(currentVersion.status.name))
          put("AGREEMENT_STATUS", listOf(currentVersion.agreementStatus.name))
          currentVersion.agreementDate?.let { agreementDate -> put("AGREEMENT_DATE", listOf(agreementDate.toString())) }

        },
        timeline = null,
      )

      // post creationCommand to the AAP backend, store the returned UUID for future commands
    }


    planRepository.save(plan.apply { migrated = true })
  }

  override fun run(vararg args: String?) {
    var index = 0
    val pageSize = 25
    var hasNext = true
    while(hasNext) {
      val pageRequest = PageRequest.of(index++, pageSize)
      val batch = planRepository.findAllByMigratedFalse(pageRequest)

      if (batch.hasContent()) break

      batch.forEach { plan -> migrate(plan) }

      hasNext = batch.hasNext()
    }
  }
}