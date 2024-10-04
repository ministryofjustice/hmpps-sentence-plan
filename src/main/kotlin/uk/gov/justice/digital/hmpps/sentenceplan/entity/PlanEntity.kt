package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "plan")
@EntityListeners(AuditingEntityListener::class)
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
  var createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date", nullable = false)
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

interface PlanRepository : JpaRepository<PlanEntity, Long> {
  fun findByUuid(planUuid: UUID): PlanEntity

  @Query("select p.* from plan p inner join oasys_pk_to_plan o on p.id = o.plan_id and o.oasys_assessment_pk = :oasysAssessmentPk", nativeQuery = true)
  fun findByOasysAssessmentPk(@Param("oasysAssessmentPk") oasysAssessmentPk: String): PlanEntity?

  @Modifying
  @Transactional
  @Query("insert into oasys_pk_to_plan(oasys_assessment_pk, plan_id) values (:oasysAssessmentPk, :planId)", nativeQuery = true)
  fun createOasysAssessmentPk(@Param("oasysAssessmentPk") oasysAssessmentPk: String, @Param("planId") planId: Long)
}
