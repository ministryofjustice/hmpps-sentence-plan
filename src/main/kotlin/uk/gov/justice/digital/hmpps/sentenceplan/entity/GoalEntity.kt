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
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

  @Column(name = "area_of_need_uuid")
  val areaOfNeedUuid: UUID,

  @Column(name = "target_date")
  val targetDate: String? = null,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Column(name = "goal_order")
  val goalOrder: Int? = null,

  // this is nullable in the declaration to enable ignoring the field in JSON serialisation
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  @JsonIgnore
  val plan: PlanEntity?,

  @OneToMany(mappedBy = "goal", cascade = arrayOf(CascadeType.ALL))
  var steps: List<StepEntity>? = emptyList(),
)

interface GoalRepository : JpaRepository<GoalEntity, Long> {
  fun findByUuid(uuid: UUID): GoalEntity?

  fun findByPlan(plan: PlanEntity): List<GoalEntity>

  @Query(
    "select g.* from goal g, 'sentence-plan'.area_of_need aon where aon.uuid = g.area_of_need_uuid and aon.name=:areaOfNeedName;",
    nativeQuery = true,
  )
  fun findByAreaOfNeed(@Param("areaOfNeedName") areaOfNeedName: String): Set<GoalEntity>

  @Query("select g from Goal g where g.areaOfNeedUuid = :#{#areaOfNeed.uuid}")
  fun getGoalsByAreaOfNeed(@Param("areaOfNeed") areaOfNeed: AreaOfNeedEntity): Set<GoalEntity>

  @Query(
    "select g.* from 'sentence-plan'.goal g" +
      "    inner join 'sentence-plan'.related_area_of_need raon on g.uuid = raon.goal_uuid" +
      "    inner join 'sentence-plan'.area_of_need aon on raon.area_of_need_uuid = aon.uuid" +
      "    where aon.uuid = :#{#areaOfNeed.uuid}",
    nativeQuery = true,
  )
  fun getGoalsByRelatedAreaOfNeed(@Param("areaOfNeed") areaOfNeed: AreaOfNeedEntity): Set<GoalEntity>

  @Modifying
  @Query("update Goal g set g.goalOrder = ?1 where g.uuid = ?2")
  fun updateGoalOrder(goalOrder: Int, uuid: UUID)
}
