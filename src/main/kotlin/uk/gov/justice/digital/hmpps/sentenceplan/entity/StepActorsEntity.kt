package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity(name = "StepActors")
@Table(name = "step_actors")
class StepActorsEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "step_uuid")
  val stepUuid: UUID,

  @Column(name = "actor")
  val actor: String,

  @Column(name = "actor_option_id")
  val actorOptionId: Int,

)

interface StepActorRepository : JpaRepository<StepActorsEntity, Long> {
  fun findById(id: Id): StepActorsEntity?
}
