package uk.gov.justice.digital.hmpps.sentenceplan.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import java.util.UUID

/**
 * This class is for methods that need to handle exceptions in the repository layer.
 * All JPA methods should be in the [uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository] interface.
 */
interface PlanVersionExceptionHandlingRepository {
  fun getVersionByUuidAndVersion(planUuid: UUID, versionNumber: Int): PlanVersionEntity
}

@Repository
class PlanVersionExceptionHandlingRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager,
) : PlanVersionExceptionHandlingRepository {
  override fun getVersionByUuidAndVersion(planUuid: UUID, versionNumber: Int): PlanVersionEntity {
    try {
      return entityManager.createQuery(
        """
    SELECT pv
    FROM PlanVersion pv
    WHERE pv.planId = (
        SELECT p.id
        FROM PlanEntity p
        WHERE p.uuid = :planUuid
    ) AND pv.version = :versionNumber
    """,
      )
        .setParameter("planUuid", planUuid)
        .setParameter("versionNumber", versionNumber)
        .singleResult as PlanVersionEntity
    } catch (e: NoResultException) {
      throw NotFoundException("Plan version $versionNumber not found for Plan uuid $planUuid")
    }
  }
}
