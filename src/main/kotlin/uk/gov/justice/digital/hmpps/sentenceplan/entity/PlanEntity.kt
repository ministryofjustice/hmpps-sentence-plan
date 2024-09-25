package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

@Entity(name = "Plan")
@Table(name = "plan")
@EntityListeners(AuditingEntityListener::class)
class PlanEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "countersigning_status")
  @Enumerated(EnumType.STRING)
  val status: CountersigningStatus = CountersigningStatus.INCOMPLETE,

  @Column(name = "agreement_status")
  @Enumerated(EnumType.STRING)
  var agreementStatus: PlanStatus = PlanStatus.DRAFT,

  @CreatedDate
  @Column(name = "creation_date")
  val creationDate: Instant = Instant.now(),

  @LastModifiedDate
  @Column(name = "updated_date")
  var updatedDate: Instant = Instant.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "updated_by_id")
  var updatedBy: PractitionerEntity? = null,

  @Column(name = "agreement_date")
  var agreementDate: Instant? = null,

  @OneToOne(mappedBy = "plan", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val agreementNote: PlanAgreementNoteEntity? = null,

  @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val planProgressNotes: Set<PlanProgressNoteEntity> = emptySet(),

  @OneToMany(mappedBy = "plan")
  @OrderBy("goalOrder ASC")
  val goals: Set<GoalEntity> = emptySet(),
)

enum class CountersigningStatus {
  INCOMPLETE,
  COMPLETE,
  LOCKED,
  SIGNED,
}

enum class PlanStatus {
  DRAFT,
  AGREED,
  DO_NOT_AGREE,
  COULD_NOT_ANSWER,
}

interface PlanRepository : JpaRepository<PlanEntity, Long> {
  fun findByUuid(uuid: UUID): PlanEntity?

  @Query("select p.* from plan p inner join oasys_pk_to_plan o on p.uuid = o.plan_uuid and o.oasys_assessment_pk = :oasysAssessmentPk", nativeQuery = true)
  fun findByOasysAssessmentPk(@Param("oasysAssessmentPk") oasysAssessmentPk: String): PlanEntity?

  @Modifying
  @Transactional
  @Query("insert into oasys_pk_to_plan(oasys_assessment_pk, plan_uuid) values (:oasysAssessmentPk, :planUuid)", nativeQuery = true)
  fun createOasysAssessmentPk(@Param("oasysAssessmentPk") oasysAssessmentPk: String, @Param("planUuid") planUuid: UUID)
}
