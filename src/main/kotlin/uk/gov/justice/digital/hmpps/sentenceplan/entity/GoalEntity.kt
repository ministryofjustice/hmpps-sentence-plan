package uk.gov.justice.digital.hmpps.sentenceplan.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID

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

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "goal_order")
  val goalOrder: Int,

)

interface GoalRepository : JpaRepository<GoalEntity, UUID> {
  override fun findById(uuid: UUID): Optional<GoalEntity>

  @Modifying
  @Query("update Goal g set g.goalOrder = ?1 where g.uuid = ?2")
  fun updateGoalOrder(goalOrder: Int, uuid: UUID)
}
