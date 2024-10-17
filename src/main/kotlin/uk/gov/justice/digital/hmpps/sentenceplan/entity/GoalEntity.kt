package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "Goal")
@Table(name = "goal")
@EntityListeners(AuditingEntityListener::class)
class GoalEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  var id: Long? = null,

  @Column(name = "uuid")
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "title")
  var title: String,

  @ManyToOne
  @JoinColumn(name = "area_of_need_id", nullable = false)
  val areaOfNeed: AreaOfNeedEntity,

  @Column(name = "target_date")
  var targetDate: LocalDate? = null,

  @Column(name = "created_date")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date")
  var updatedDate: LocalDateTime = LocalDateTime.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "last_updated_by_id")
  var updatedBy: PractitionerEntity? = null,

  @Column(name = "goal_status")
  @Enumerated(EnumType.STRING)
  var status: GoalStatus = GoalStatus.FUTURE,

  @Column(name = "status_date")
  var statusDate: LocalDateTime? = null,

  // this is nullable in the declaration to enable ignoring the field in JSON serialisation
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_version_id", nullable = false)
  @JsonIgnore
  var planVersion: PlanVersionEntity?,

  @Column(name = "goal_order")
  val goalOrder: Int = 0,

  @OneToMany(mappedBy = "goal", cascade = [CascadeType.ALL])
  @OrderBy("createdDate ASC")
  var steps: List<StepEntity> = emptyList(),

  @OneToMany(mappedBy = "goal", cascade = [CascadeType.ALL])
  @OrderBy("createdDate ASC")
  var notes: MutableSet<GoalNoteEntity> = mutableSetOf(),

  @ManyToMany
  @JoinTable(
    name = "related_area_of_need",
    joinColumns = [JoinColumn(name = "goal_id")],
    inverseJoinColumns = [JoinColumn(name = "area_of_need_id")],
    uniqueConstraints = [UniqueConstraint(columnNames = ["goal_id", "area_of_need_id"])],
  )
  var relatedAreasOfNeed: MutableSet<AreaOfNeedEntity>? = mutableSetOf(),
) {

  fun merge(goal: Goal, relatedAreasOfNeedList: List<AreaOfNeedEntity>): GoalEntity {
    if (goal.title != null) {
      this.title = goal.title
    }

    if (goal.targetDate != null) {
      this.targetDate = LocalDate.parse(goal.targetDate)
      if (this.status == GoalStatus.FUTURE) {
        this.status = GoalStatus.ACTIVE
        this.statusDate = LocalDateTime.now()
      }
    }

    if (goal.targetDate == null && goal.status == GoalStatus.FUTURE) {
      this.targetDate = null
    }

    // If we receive a goal note, check if there is an ACHIEVED or REMOVED status and create a note with that.
    // Otherwise make it a progress note
    goal.note?.let { note ->
      val goalNoteEntity = GoalNoteEntity(note = note, goal = this).apply {
        type = when (goal.status) {
          GoalStatus.REMOVED -> GoalNoteType.REMOVED
          GoalStatus.ACHIEVED -> GoalNoteType.ACHIEVED
          else -> GoalNoteType.PROGRESS
        }
      }

      goalNoteEntity.type.let {
        this.notes.add(goalNoteEntity)
      }
    }

    goal.status?.let { status ->
      this.status = status
      this.statusDate = LocalDateTime.now()
    }

    this.relatedAreasOfNeed = relatedAreasOfNeedList.toMutableSet()

    return this
  }
}

enum class GoalStatus {
  ACTIVE,
  FUTURE,
  ACHIEVED,
  REMOVED,
}

interface GoalRepository : JpaRepository<GoalEntity, Long> {
  fun findByUuid(uuid: UUID): GoalEntity?

  // returns how many records were deleted
  @Modifying
  @Query("DELETE Goal g WHERE g.uuid = :goalUuid")
  fun deleteByUuid(goalUuid: UUID): Int

  @Query("select g from Goal as g join fetch g.steps where g.uuid = :uuid")
  fun findByUuidWithSteps(uuid: UUID): GoalEntity

  @Modifying
  @Query("update Goal g set g.goalOrder = ?1 where g.uuid = ?2")
  fun updateGoalOrder(goalOrder: Int, uuid: UUID)
}
