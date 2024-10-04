package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
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
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "PlanProgressNote")
@Table(name = "plan_progress_notes")
@EntityListeners(AuditingEntityListener::class)
class PlanProgressNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  var id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_version_id", referencedColumnName = "id")
  @JsonIgnore
  var planVersion: PlanVersionEntity?,

  @Column(name = "note")
  var note: String,

  @Column(name = "is_support_needed")
  @Enumerated(EnumType.STRING)
  var isSupportNeeded: NoteIsSupportNeeded,

  @Column(name = "is_support_needed_note")
  var isSupportNeededNote: String,

  @Column(name = "is_involved")
  var isInvolved: Boolean,

  @Column(name = "is_involved_note")
  var isInvolvedNote: String,

  @Column(name = "person_name")
  var personName: String,

  @Column(name = "practitioner_name")
  var practitionerName: String,

  @Column(name = "created_date")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,
)

enum class NoteIsSupportNeeded {
  YES,
  NO,
  DONT_KNOW,
}

interface PlanProgressNotesRepository : JpaRepository<PlanProgressNoteEntity, Long> {
  fun findByPlanVersionUuid(planVersionUuid: UUID): List<PlanProgressNoteEntity>
}
