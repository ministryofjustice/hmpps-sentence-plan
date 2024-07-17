package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
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

  @Column(name = "plan_uuid")
  var planUuid: UUID? = null,
)

interface GoalRepository : JpaRepository<GoalEntity, Long> {
  fun findByUuid(uuid: UUID): GoalEntity?

  fun findByPlanUuid(planUuid: UUID): List<GoalEntity>

  @Query("select g.* from goal g, area_of_need aon where aon.uuid = g.area_of_need_uuid and aon.name=:areaOfNeedName;", nativeQuery = true)
  fun findByAreaOfNeed(@Param("areaOfNeedName") areaOfNeedName: String): Set<GoalEntity>

  @Query("select g from Goal g where g.areaOfNeedUuid = :#{#areaOfNeed.uuid}")
  fun getGoalsByAreaOfNeed(@Param("areaOfNeed") areaOfNeed: AreaOfNeedEntity): Set<GoalEntity>

  @Query(
    "select g.* from goal g" +
      "    inner join related_area_of_need raon on g.uuid = raon.goal_uuid" +
      "    inner join area_of_need aon on raon.area_of_need_uuid = aon.uuid" +
      "    where aon.uuid = :#{#areaOfNeed.uuid}",
    nativeQuery = true,
  )
  fun getGoalsByRelatedAreaOfNeed(@Param("areaOfNeed")areaOfNeed: AreaOfNeedEntity): Set<GoalEntity>

  @Modifying
  @Query("update Goal g set g.goalOrder = ?1 where g.uuid = ?2")
  fun updateGoalOrder(goalOrder: Int, uuid: UUID)
}
