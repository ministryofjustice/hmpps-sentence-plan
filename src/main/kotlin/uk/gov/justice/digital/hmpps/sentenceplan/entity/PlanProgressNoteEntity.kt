package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

@Entity(name = "PlanProgressNote")
@Table(name = "plan_progress_notes")
class PlanProgressNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_version_id", referencedColumnName = "id")
  @JsonIgnore
  val planVersion: PlanVersionEntity?,

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

interface PlanProgressNotesRepository : JpaRepository<PlanProgressNoteEntity, Long>
