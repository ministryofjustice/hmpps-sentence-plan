package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "Step")
@Table(name = "step")
class StepEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "description")
  val description: String,

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  val status: StepStatus = StepStatus.NOT_STARTED,

  @Column(name = "creation_date", columnDefinition = "TIMESTAMP")
  val creationDate: LocalDateTime = LocalDateTime.now(),

  // this is nullable in the declaration to enable ignoring the field in JSON serialisation
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "goal_id", nullable = false)
  @JsonIgnore
  val goal: GoalEntity? = null,

  @Column(name = "actor", nullable = false)
  var actor: String,
)

enum class StepStatus {
  NOT_STARTED,
  IN_PROGRESS,
  COMPLETED,
  CANNOT_BE_DONE_YET,
  NO_LONGER_NEEDED,
}

interface StepRepository : JpaRepository<StepEntity, Long> {
  fun findByUuid(uuid: UUID): StepEntity?
}
