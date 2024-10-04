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
import java.util.UUID

@Service
class VersionService(
  private val planVersionRepository: PlanVersionRepository,
) {
  @PersistenceContext
  lateinit var entityManager: EntityManager

  // we keep a list of plans being copied so that cascading persists don't trigger recursive copying
  private var planVersionsBeingCopied: MutableSet<UUID> = mutableSetOf()

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

    planVersionsBeingCopied.add(newPlanVersionEntity.uuid)

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
    planVersionsBeingCopied.remove(newPlanVersionEntity.uuid)

    entityManager.detach(newPlanVersionEntity)

    val currentPlanVersion = planVersionRepository.findByUuid(planVersionUuid)
    currentPlanVersion.version = currentPlanVersion.version.inc()
    val updatedCurrentVersion = planVersionRepository.save(currentPlanVersion)

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

    // We only allow a single new version of a plan to be made at once.
    // This is because when a new PlanVersion is being created the @PrePersist on its children will be triggered,
    // which if unchecked-for will result in infinite recursion.
    // We have not implemented a way of differentiating between human-triggerd and machine-triggered versioning.
    // This means we also do not provide any kind of object invalidation back to the user. If two users modify the same
    // plan version in short succession then the first change will be overridden.
    if (planVersionsBeingCopied.contains(planVersion.uuid)) {
      return planVersion
    }

    val planVersionUuid: UUID = planVersion.uuid

    planVersionsBeingCopied.add(planVersionUuid)
    val currentPlanVersion = createNewPlanVersion(planVersionUuid)
    planVersionsBeingCopied.remove(planVersionUuid)

    return currentPlanVersion
  }
}
