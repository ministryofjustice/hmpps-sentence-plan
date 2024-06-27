package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

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

  @Column(name = "status")
  val status: PlanStatus,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "updated_date")
  val updatedDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
)

enum class PlanStatus {
  INCOMPLETE,
  COMPLETE,
  LOCKED,
  SIGNED,
}

@Converter(autoApply = true)
class PlanStatusConverter : AttributeConverter<PlanStatus, String> {
  override fun convertToDatabaseColumn(status: PlanStatus): String = status.name

  override fun convertToEntityAttribute(status: String): PlanStatus = PlanStatus.valueOf(status.uppercase())
}

interface PlanRepository : JpaRepository<PlanEntity, Long> {
  fun findByUuid(uuid: UUID): PlanEntity?

  @Query("select p.* from plan p inner join oasys_pk_to_plan o on p.uuid = o.plan_uuid and o.oasys_assessment_pk = :oasysAssessmentPk", nativeQuery = true)
  fun findByOasysAssessmentPk(@Param("oasysAssessmentPk") oasysAssessmentPk: String): PlanEntity?
}
