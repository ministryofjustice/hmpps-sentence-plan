package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EntityManager
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.OneToOne
import jakarta.persistence.PersistenceContext
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sentenceplan.data.Note
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "plan")
@EntityListeners(AuditingEntityListener::class)
@NamedNativeQuery(
  name = "PlanEntity.getPlanAndGoalNotes",
  query = """
select 'Plan' as note_object,
       plan_notes.agreement_status_note as note,
       plan_notes.optional_note as additional_note,
       CAST(plan_notes.agreement_status AS VARCHAR) AS note_type,
       null as goal_title,
       null as goal_uuid,
       null as goal_status,
       plan_notes.created_date,
       practitioner.username as created_by
from "sentence-plan".plan_agreement_notes plan_notes
    inner join "sentence-plan".practitioner on plan_notes.created_by_id = practitioner.id
    inner join "sentence-plan".plan_version on plan_notes.plan_version_id = plan_version.id
    inner join "sentence-plan".plan on plan.current_plan_version_id = plan_version.id
    and plan.uuid = ?1

UNION ALL

select 'Goal' as note_object,
       goal_notes.note,
       null as additional_note,
       CAST(goal_notes.note_type AS VARCHAR) AS note_type,
       goal.title as goal_title,
       CAST(goal.uuid AS VARCHAR) as goal_uuid, 
       CAST(goal.goal_status AS VARCHAR) as goal_status,
       goal_notes.created_date,
       practitioner.username as created_by
from "sentence-plan".goal_notes
    inner join "sentence-plan".practitioner on goal_notes.created_by_id = practitioner.id
    inner join "sentence-plan".goal on goal_notes.goal_id = goal.id
    inner join "sentence-plan".plan_version on goal.plan_version_id = plan_version.id
    inner join "sentence-plan".plan on plan.current_plan_version_id = plan_version.id
    and plan.uuid = ?1
        
    ORDER BY created_date DESC;
    """,
  resultSetMapping = "NoteMapping",
)
@SqlResultSetMapping(
  name = "NoteMapping",
  classes = [
    ConstructorResult(
      targetClass = Note::class,
      columns = [
        ColumnResult(name = "note_object"),
        ColumnResult(name = "note"),
        ColumnResult(name = "additional_note"),
        ColumnResult(name = "note_type"),
        ColumnResult(name = "goal_title"),
        ColumnResult(name = "goal_uuid"),
        ColumnResult(name = "goal_status"),
        ColumnResult(name = "created_date", type = LocalDateTime::class),
        ColumnResult(name = "created_by"),
      ],
    ),
  ],
)
class PlanEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  @JsonIgnore
  var id: Long? = null,

  @Column(name = "published_state")
  @Enumerated(EnumType.STRING)
  var publishedState: PublishState = PublishState.UNPUBLISHED,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID = UUID.randomUUID(),

  @CreatedDate
  @Column(name = "created_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  var createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date", nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  var lastUpdatedDate: LocalDateTime = LocalDateTime.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "last_updated_by_id")
  var lastUpdatedBy: PractitionerEntity? = null,

  // this is nullable because PlanEntity and PlanVersionEntity link to each other. We must have a PlanEntity.ID before we can save a PlanVersionEntity
  @OneToOne()
  @JoinColumn(name = "current_plan_version_id")
  var currentVersion: PlanVersionEntity? = null,
)

enum class PublishState {
  UNPUBLISHED,
  PUBLISHED,
  ARCHIVED,
}

@Repository
interface PlanEntityRepository :
  JpaRepository<PlanEntity, Long>,
  PlanEntityExceptionHandlingRepository {

  // this uses the NoteMapping annotated on the PlanEntity class above
  @Query(nativeQuery = true)
  fun getPlanAndGoalNotes(planUuid: UUID): List<Note>
}

interface PlanEntityExceptionHandlingRepository {
  fun getByUuid(planUuid: UUID): PlanEntity
}

@Repository
class PlanEntityExceptionHandlingRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager,
) : PlanEntityExceptionHandlingRepository {
  override fun getByUuid(planUuid: UUID): PlanEntity {
    try {
      return entityManager.createQuery(
        """
        SELECT p
        FROM PlanEntity p
        WHERE p.uuid = :planUuid
        """,
      )
        .setParameter("planUuid", planUuid)
        .singleResult as PlanEntity
    } catch (e: EmptyResultDataAccessException) {
      throw NotFoundException("Plan not found for id $planUuid")
    }
  }
}
