package uk.gov.justice.digital.hmpps.sentenceplan.services

import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.getNextPlanVersion
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class VersionService(
  private val planVersionRepository: PlanVersionRepository,
) {
  @PersistenceContext
  lateinit var entityManager: EntityManager

  /**
   * This function makes a copy of a PlanVersion object and all its descendent objects.
   * New UUIDs are set on the copied objects and then persisted to the database.
   */
  private fun createNewPlanVersion(planVersionUuid: UUID): PlanVersionEntity {
    val newPlanVersionEntity: PlanVersionEntity

    try {
      newPlanVersionEntity = planVersionRepository.getWholePlanVersionByUuid(planVersionUuid)
    } catch (_: NoResultException) {
      throw NoResultException("A Plan Version couldn't be found for Plan Version UUID: $planVersionUuid")
    }

    entityManager.detach(newPlanVersionEntity)

    newPlanVersionEntity.uuid = UUID.randomUUID()

    newPlanVersionEntity.id = null
    newPlanVersionEntity.agreementNote?.id = null
    newPlanVersionEntity.agreementNote?.planVersion = newPlanVersionEntity

    newPlanVersionEntity.planProgressNotes.forEach { planProgressNote ->
      planProgressNote.id = null
      planProgressNote.planVersion = newPlanVersionEntity
    }

    newPlanVersionEntity.goals.forEach { goal ->
      entityManager.detach(goal)
      goal.id = null
      goal.uuid = UUID.randomUUID()
      goal.planVersion = newPlanVersionEntity

      val stepsList: List<StepEntity> = goal.steps.toList() // copy the list
      stepsList.forEach { step ->
        entityManager.detach(step)
        step.id = null
        step.uuid = UUID.randomUUID()
        step.goal = goal
      }
      goal.steps = stepsList

      // the set needs copying otherwise both original and new goal will be referencing the same collection object
      val relatedAreasSet: MutableSet<AreaOfNeedEntity>? = goal.relatedAreasOfNeed?.toSet()?.toMutableSet()
      goal.relatedAreasOfNeed = relatedAreasSet
    }

    planVersionRepository.save(newPlanVersionEntity)

    entityManager.detach(newPlanVersionEntity)

    val currentPlanVersion = planVersionRepository.findByUuid(planVersionUuid)
    currentPlanVersion.version = planVersionRepository.getNextPlanVersion(currentPlanVersion.planId)
    val updatedCurrentVersion = planVersionRepository.save(currentPlanVersion)

    entityManager.flush()

    return updatedCurrentVersion
  }

  @Transactional
  fun conditionallyCreateNewPlanVersion(planVersion: PlanVersionEntity?): PlanVersionEntity {
    if (planVersion == null) {
      throw NullPointerException("Tried to create a new plan version for a null planVersion")
    }

    // Don't try and make a new version if the passed-in reference hasn't been saved yet.
    if (planVersion.id == null) {
      return planVersion
    }

    // Do not make a new version if this PlanVersion is not the latest version - the persist which triggered this will
    // still take place.
    // If we want to prevent the persist we can return a value here, detect it in the @PrePersist and throw an e.g. RuntimeException
    if (planVersion.uuid != planVersion.plan?.currentVersion?.uuid) {
      return planVersion
    }

    return if (LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).isAfter(planVersion.updatedDate)) {
      createNewPlanVersion(planVersion.uuid)
    } else {
      planVersion
    }
  }

  /**
   * Always creates a new PlanVersion, regardless of the criteria that apply in `conditionallyCreateNewPlanVersion`
   */
  @Transactional
  fun alwaysCreateNewPlanVersion(planVersion: PlanVersionEntity): PlanVersionEntity {
    // Don't try and make a new version if the passed-in reference hasn't been saved yet.
    if (planVersion.id == null) {
      return planVersion
    }
    return createNewPlanVersion(planVersion.uuid)
  }
}
