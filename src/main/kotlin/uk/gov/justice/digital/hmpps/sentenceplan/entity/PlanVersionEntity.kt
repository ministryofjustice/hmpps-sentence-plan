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
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "PlanVersion")
@Table(name = "plan_version")
@EntityListeners(AuditingEntityListener::class)
class PlanVersionEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "version")
  var version: Int = 0,

  // this is nullable in the declaration to enable ignoring the field in JSON serialisation
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  @JsonIgnore
  val plan: PlanEntity?,

  @Column(name = "plan_type", nullable = false)
  @Enumerated(EnumType.STRING)
  var planType: PlanType = PlanType.INITIAL,

  @Column(name = "countersigning_status")
  @Enumerated(EnumType.STRING)
  val status: CountersigningStatus = CountersigningStatus.UNSIGNED,

  @Column(name = "agreement_status")
  @Enumerated(EnumType.STRING)
  var agreementStatus: PlanAgreementStatus = PlanAgreementStatus.DRAFT,

  @CreatedDate
  @Column(name = "created_date")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date")
  var updatedDate: LocalDateTime = LocalDateTime.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "last_updated_by_id")
  var updatedBy: PractitionerEntity? = null,

  @Column(name = "agreement_date")
  var agreementDate: LocalDateTime? = null,

  @Column(name = "read_only")
  var readOnly: Boolean = false,

  @Column(name = "checksum")
  var checksum: String? = null,

  @OneToOne(mappedBy = "planVersion", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val agreementNote: PlanAgreementNoteEntity? = null,

  @OneToMany(mappedBy = "planVersion", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val planProgressNotes: Set<PlanProgressNoteEntity> = emptySet(),

  @OneToMany(mappedBy = "planVersion")
  @OrderBy("goalOrder ASC")
  val goals: Set<GoalEntity> = emptySet(),
)

enum class CountersigningStatus {
  AWAITING_COUNTERSIGN,
  AWAITING_DOUBLE_COUNTERSIGN,
  COUNTERSIGNED,
  DOUBLE_COUNTERSIGNED,
  LOCKED_INCOMPLETE,
  REJECTED,
  ROLLED_BACK,
  SELF_SIGNED,
  UNSIGNED,
}

enum class PlanAgreementStatus {
  DRAFT,
  AGREED,
  DO_NOT_AGREE,
  COULD_NOT_ANSWER,
}

enum class PlanType {
  INITIAL,
  REVIEW,
  TERMINATE,
  TRANSFER,
  OTHER,
}

interface PlanVersionRepository : JpaRepository<PlanVersionEntity, Long> {
  fun findByUuid(planVersionUuid: UUID): PlanVersionEntity
}
