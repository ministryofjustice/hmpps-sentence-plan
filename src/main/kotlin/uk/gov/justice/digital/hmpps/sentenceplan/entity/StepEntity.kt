package uk.gov.justice.digital.hmpps.sentenceplan.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID

@Entity(name = "Step")
@Table(name = "step")
class StepEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "related_goal_id")
  var relatedGoalId: UUID? = null,

  @Column(name = "description")
  val description: String,

  @Column(name = "actor")
  val actor: String,

  @Column(name = "status")
  val status: String,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

)

interface StepRepository : JpaRepository<StepEntity, UUID> {
  override fun findById(uuid: UUID): Optional<StepEntity>
  fun findAllByRelatedGoalId(relatedGoalId: UUID): Optional<List<StepEntity>>
}
