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

    if (newPlanVersionEntity.agreementNote != null) {
      newPlanVersionEntity.agreementNote.id = null
      newPlanVersionEntity.agreementNote.planVersion = newPlanVersionEntity
    }

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

    planVersionRepository.save(newPlanVersionEntity)
    entityManager.detach(newPlanVersionEntity)

    val currentPlanVersion = planVersionRepository.findByUuid(planVersionUuid)
    currentPlanVersion.version = currentPlanVersion.version.inc()
    val updatedCurrentVersion = planVersionRepository.save(currentPlanVersion)

    return updatedCurrentVersion
  }
}
