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

  /**
   * This function makes a copy of a PlanVersion object and all its descendent objects.
   * New UUIDs are set on the copied objects and then persisted to the database.
   */
  @Transactional
  fun createNewPlanVersion(planVersionUuid: UUID): PlanVersionEntity {
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

      val relatedAreasSet: MutableSet<AreaOfNeedEntity>? = goal.relatedAreasOfNeed?.toSet()?.toMutableSet()
      goal.relatedAreasOfNeed = relatedAreasSet
    }

    planVersionsBeingCopied.add(newPlanVersionEntity.uuid)
    planVersionRepository.save(newPlanVersionEntity)
    planVersionsBeingCopied.remove(newPlanVersionEntity.uuid)

    entityManager.detach(newPlanVersionEntity)

    val currentPlanVersion = planVersionRepository.findByUuid(planVersionUuid)
    currentPlanVersion.version = currentPlanVersion.version.inc()
    val updatedCurrentVersion = planVersionRepository.save(currentPlanVersion)

    return updatedCurrentVersion
  }

  private var planVersionsBeingCopied: MutableSet<UUID> = mutableSetOf()

  @Transactional
  fun doStuff(planVersion: PlanVersionEntity?) {
    println("In VersionService doStuff")
    if (planVersion == null) {
      return
    }

    // do nothing if this PlanVersion is not the latest version - this will not prevent changes to the objects however!
    // if we want to prevent the persist we can return a value here, detect it in the @PrePersist and throw an e.g. RuntimeException
    if (planVersion.uuid != planVersion.plan?.currentVersion?.uuid) {
      return
    }

    if (planVersionsBeingCopied.contains(planVersion.uuid)) {
      return
    }

    planVersionsBeingCopied.add(planVersion.uuid)
    createNewPlanVersion(planVersion.uuid)
    planVersionsBeingCopied.remove(planVersion.uuid)
  }
}
