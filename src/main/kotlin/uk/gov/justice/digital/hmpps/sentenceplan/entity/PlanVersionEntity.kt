package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.Formula
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.sentenceplan.repository.PlanVersionExceptionHandlingRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "PlanVersion")
@Table(name = "plan_version")
@EntityListeners(AuditingEntityListener::class)
@NamedEntityGraph(
  name = "graph.planversion.eager",
  attributeNodes = [
    NamedAttributeNode("agreementNotes"),
    NamedAttributeNode("planProgressNotes"),
    NamedAttributeNode(value = "goals", subgraph = "goals-subgraph"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "goals-subgraph",
      attributeNodes = [
        NamedAttributeNode("steps"),
        NamedAttributeNode("notes"),
        NamedAttributeNode("relatedAreasOfNeed"),
      ],
    ),
  ],
)
class PlanVersionEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  var id: Long? = null,

  @Column(name = "uuid")
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "version")
  var version: Int = 0,

  // This is nullable in the declaration to enable ignoring the field in JSON serialisation
  @OneToOne(mappedBy = "currentVersion")
  @JsonIgnore
  val plan: PlanEntity?,

  // We need this field as well as the plan above because we want a reference from each of the plan and plan_version tables
  @Column(name = "plan_id")
  val planId: Long,

  @Column(name = "plan_type", nullable = false)
  @Enumerated(EnumType.STRING)
  var planType: PlanType = PlanType.INITIAL,

  @Column(name = "countersigning_status")
  @Enumerated(EnumType.STRING)
  var status: CountersigningStatus = CountersigningStatus.UNSIGNED,

  @Column(name = "agreement_status")
  @Enumerated(EnumType.STRING)
  var agreementStatus: PlanAgreementStatus = PlanAgreementStatus.DRAFT,

  @CreatedDate
  @Column(name = "created_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  var updatedDate: LocalDateTime = LocalDateTime.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "last_updated_by_id")
  var updatedBy: PractitionerEntity? = null,

  @Column(name = "agreement_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  var agreementDate: LocalDateTime? = null,

  @Column(name = "read_only")
  var readOnly: Boolean = false,

  @Column(name = "checksum")
  var checksum: String? = null,

  @OneToMany(mappedBy = "planVersion", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  val agreementNotes: Set<PlanAgreementNoteEntity> = emptySet(),

  @OneToMany(mappedBy = "planVersion", cascade = [CascadeType.ALL])
  val planProgressNotes: Set<PlanProgressNoteEntity> = emptySet(),

  @OneToMany(mappedBy = "planVersion", cascade = [CascadeType.PERSIST], fetch = FetchType.EAGER)
  @OrderBy("goalOrder ASC")
  val goals: Set<GoalEntity> = emptySet(),

  @Column(name = "soft_deleted")
  var softDeleted: Boolean = false,

  @Formula("GREATEST(last_updated_date, (SELECT MAX(g.last_updated_date) FROM goal g WHERE g.plan_version_id = id))")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  var mostRecentUpdateDate: LocalDateTime = LocalDateTime.now(),

  // this query retrieves the username directly because Formula can only retrieve scalar values, not objects.
  @Formula(
    """
    (select p.username FROM practitioner p
    WHERE p.id = (
      SELECT COALESCE(
        (SELECT g.last_updated_by_id
        FROM goal g
        WHERE g.plan_version_id = id
        ORDER BY g.last_updated_date DESC
        LIMIT 1),
        last_updated_by_id
      )
    ))
  """,
  )
  var mostRecentUpdateByName: String? = null,

  @Transient
  @JsonProperty("crn")
  val crn: String? = null, // used for deserializing from JSON
) {
  @get:JsonProperty("crn")
  val jsonCrn: String?
    get() = plan?.crn // used for serializing to JSON
}

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
  UPDATED_AGREED,
  UPDATED_DO_NOT_AGREE,
}

enum class PlanType {
  INITIAL,
  REVIEW,
  TERMINATE,
  TRANSFER,
  OTHER,
}

@Repository
interface PlanVersionRepository :
  JpaRepository<PlanVersionEntity, Long>,
  PlanVersionExceptionHandlingRepository {
  fun findByUuid(planVersionUuid: UUID): PlanVersionEntity

  @Query(
    """
    SELECT COALESCE(MAX(pv.version) + 1, 0)
    FROM PlanVersion pv
    WHERE pv.planId = :planId
    """,
  )
  fun findNextPlanVersion(planId: Long): Int

  fun findFirstByPlanIdAndSoftDeletedOrderByVersionDesc(planId: Long, softDeleted: Boolean): PlanVersionEntity?

  fun findAllByPlanId(planId: Long): List<PlanVersionEntity>

  fun findAllByPlanUuid(planUuid: UUID): List<PlanVersionEntity>

  @EntityGraph(value = "graph.planversion.eager", type = EntityGraph.EntityGraphType.FETCH)
  fun getWholePlanVersionByUuid(planVersionUuid: UUID): PlanVersionEntity
}
