package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(name = "Plan")
@Table(name = "plan")
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

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "updated_date")
  val updatedDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "agreement_date")
  val agreementDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

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
  COULD_NOT_AGREE
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
