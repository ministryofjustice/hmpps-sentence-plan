package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(name = "Goal")
@Table(name = "goal")
class GoalEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "title")
  val title: String,

  @ManyToOne
  @JoinColumn(name = "area_of_need_id", nullable = false)
  val areaOfNeed: AreaOfNeedEntity,

  @Column(name = "target_date")
  val targetDate: String? = null,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "goal_order")
  val goalOrder: Int = 0,

  // this is nullable in the declaration to enable ignoring the field in JSON serialisation
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  @JsonIgnore
  val plan: PlanEntity?,

  @OneToMany(mappedBy = "goal", cascade = [CascadeType.ALL])
  @OrderBy("creationDate ASC")
  var steps: List<StepEntity> = emptyList(),

  @ManyToMany
  @JoinTable(
    name = "related_area_of_need",
    joinColumns = [JoinColumn(name = "goal_id")],
    inverseJoinColumns = [JoinColumn(name = "area_of_need_id")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["goal_id", "area_of_need_id"])],
  )
  val relatedAreasOfNeed: List<AreaOfNeedEntity>? = emptyList(),
)

interface GoalRepository : JpaRepository<GoalEntity, Long> {
  fun findByUuid(uuid: UUID): GoalEntity?

  fun findByPlan(plan: PlanEntity): List<GoalEntity>

  @Modifying
  @Query("update Goal g set g.goalOrder = ?1 where g.uuid = ?2")
  fun updateGoalOrder(goalOrder: Int, uuid: UUID)
}
