package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

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

  @Column(name = "plan_uuid")
  var planUuid: UUID,

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
