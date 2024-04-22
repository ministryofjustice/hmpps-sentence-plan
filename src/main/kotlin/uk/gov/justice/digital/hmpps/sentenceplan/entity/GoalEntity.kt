package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.google.gson.annotations.SerializedName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Entity(name = "Goal")
@Table(name = "goal")
class GoalEntity(
  @Id
  @SerializedName("id") val id: Int,

  @Column(name = "title")
  @SerializedName("title") val title: String,

  @Column(name = "area_of_need")
  @SerializedName("areaOfNeed") val areaOfNeed: String,

  @Column(name = "target_date")
  @SerializedName("targetDate") val targetDate: String,

  @Column(name = "is_agreed")
  @SerializedName("isAgreed") val isAgreed: Boolean,

  @Column(name = "agreement_note")
  @SerializedName("agreementNote") val agreementNote: String,

)

interface GoalRepository : JpaRepository<GoalEntity, Int> {
  override fun findById(id: Int): Optional<GoalEntity>
}
