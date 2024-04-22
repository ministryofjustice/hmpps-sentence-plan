package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.google.gson.annotations.SerializedName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Entity(name = "Step")
@Table(name = "step")
class StepEntity(
  @Id
  @SerializedName("id") val id: Int,

  @Column(name = "related_goal_id")
  @SerializedName("related_goal_id") var relatedGoalId: Int,

  @Column(name = "description")
  @SerializedName("description") val description: String,

  @Column(name = "actor")
  @SerializedName("actor") val actor: String,

  @Column(name = "status")
  @SerializedName("status") val status: String,

)

interface StepRepository : JpaRepository<StepEntity, Int> {
  override fun findById(id: Int): Optional<StepEntity>
}
