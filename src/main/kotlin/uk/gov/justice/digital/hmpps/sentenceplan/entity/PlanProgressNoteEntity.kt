package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.format.DateTimeFormatter

enum class NoteIsSupportNeeded {
  YES,
  NO,
  DONT_KNOW,
}

@Entity(name = "PlanProgressNote")
@Table(name = "plan_progress_notes")
class PlanProgressNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", referencedColumnName = "id")
  @JsonIgnore
  val plan: PlanEntity = PlanEntity(),

  @Column(name = "note")
  var note: String,

  @Column(name = "is_support_needed")
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

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
)

interface PlanProgressNotesRepository : JpaRepository<PlanProgressNoteEntity, Long>
