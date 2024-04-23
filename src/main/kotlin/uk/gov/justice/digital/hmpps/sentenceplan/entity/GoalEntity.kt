package uk.gov.justice.digital.hmpps.sentenceplan.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Entity(name = "Goal")
@Table(name = "goal")
class GoalEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID = UUID.randomUUID(),

  @Column(name = "title")
  val title: String,

  @Column(name = "area_of_need")
  val areaOfNeed: String,

  @Column(name = "target_date")
  val targetDate: String,

  @Column(name = "is_agreed")
  val isAgreed: Boolean,

  @Column(name = "agreement_note")
  val agreementNote: String,

)

interface GoalRepository : JpaRepository<GoalEntity, UUID> {
  override fun findById(uuid: UUID): Optional<GoalEntity>
}
