package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.AAPService
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.IdentifierType
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.common.UserDetails
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator.CoordinatorService
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator.MigrateAssociationRequest
import uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator.VersionMapping
import java.time.LocalDateTime
import java.util.UUID

enum class KnownPlans(val planId: Long) {
  NORMAL_PLAN(10756),
  HEAVY_VERSIONS_PLAN(3027),
}

@Component
class PlanMigrator(
  private val planRepository: PlanRepository,
  private val planVersionRepository: PlanVersionRepository,
  private val planVersionMigrator: PlanVersionMigrator,
  private val aapService: AAPService,
  private val coordinatorService: CoordinatorService,
) {
  @Transactional
  fun migrate(plan: PlanEntity) {
    log.info("Migrating plan ${plan.id}")

    val context = createContext(plan)

    var versionMappings: List<VersionMapping>

    try {
      var previousTimestamp: LocalDateTime? = null

      val versions: Map<LocalDateTime, PlanVersionEntity> = plan.id
        ?.let(planVersionRepository::findAllByPlanId)
        .orEmpty()
        .filter { !it.softDeleted }
        .groupBy { it.version }
        .map { it.key to it.value.maxBy { version -> version.id!! } }.toMap()
        .values
        .sortedBy { it.version }
        .associateBy { current ->
          val adjusted = if (previousTimestamp == null) {
            current.updatedDate
          } else if (current.updatedDate.isBefore(previousTimestamp)) {
            previousTimestamp.plusSeconds(1)
          } else {
            current.updatedDate
          }

          previousTimestamp = adjusted
          adjusted
        }

      versionMappings = versions.map {
        planVersionMigrator
          .migrate(context, it.value, it.key)
          .also { Stats.numberOfPlans += 1 }
      }

      runCatching {
        coordinatorService.migrateAssociations(
          MigrateAssociationRequest(
            mappings = versionMappings
              .mapNotNull { mapping ->
                when (mapping.event) {
                  "LOCKED_INCOMPLETE" -> mapping.apply { event = "LOCKED" }
                  "UNSIGNED" -> null
                  else -> mapping
                }
              },
            entityUuidFrom = plan.uuid,
            entityUuidTo = UUID.fromString(context.assessmentUuid),
          ),
        )
      }.onFailure { ex ->
        when (ex) {
          is WebClientResponseException.NotFound -> throw IllegalStateException("Association not found for plan ${plan.id}")
          else -> throw ex
        }
      }

      planRepository.save(plan.apply { migrated = true })
    } catch (e: Exception) {
      log.warn("Failed to migrate plan ${plan.id}: ${e.message}")
      aapService.deleteAssessment(UUID.fromString(context.assessmentUuid))
      throw e
    }
  }

  fun createContext(plan: PlanEntity): Context {
    val response =
      aapService.dispatchCommands(
        plan.createdDate,
        commands = listOf(
          CreateAssessmentCommand(
            user = UserDetails.from(plan.createdBy),
            formVersion = "v1.0",
            properties = emptyMap(),
            assessmentType = "SENTENCE_PLAN",
            timeline = null,
            identifiers = plan.crn?.let { mapOf(IdentifierType.CRN to it) },
          ),
          CreateCollectionCommand(
            name = "GOALS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = "@0",
          ),
          CreateCollectionCommand(
            name = "PLAN_AGREEMENTS",
            parentCollectionItemUuid = null,
            user = UserDetails.from(plan.createdBy),
            assessmentUuid = "@0",
          ),
        ),
      )

    return Context(
      plan = plan,
      assessmentUuid = response.extractNthInstance<CreateAssessmentCommandResult>(0).assessmentUuid,
      goalsCollectionUuid = response.extractNthInstance<CreateCollectionCommandResult>(0).collectionUuid,
      planAgreementsCollectionUuid = response.extractNthInstance<CreateCollectionCommandResult>(1).collectionUuid,
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
