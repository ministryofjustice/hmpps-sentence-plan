package uk.gov.justice.digital.hmpps.sentenceplan.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import java.util.UUID

interface PlanEntityExceptionHandlingRepository {
  fun getByUuid(planUuid: UUID): PlanEntity
}

@Repository
class PlanEntityExceptionHandlingRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager,
) : PlanEntityExceptionHandlingRepository {
  override fun getByUuid(planUuid: UUID): PlanEntity = try {
    entityManager.createQuery(
      """
        SELECT p
        FROM PlanEntity p
        WHERE p.uuid = :planUuid
        """,
      PlanEntity::class.java,
    )
      .setParameter("planUuid", planUuid)
      .singleResult
  } catch (e: NoResultException) {
    throw NotFoundException("Plan not found for id $planUuid")
  }
}
