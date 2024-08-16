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

  @Column(name = "title")
  var title: String,

  @Column(name = "text")
  var text: String,

  @Column(name = "practitioner_name")
  var practitioner_name: String,

  @Column(name = "person_name")
  var person_name: String,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
)

interface PlanProgressNotesRepository : JpaRepository<PlanProgressNoteEntity, Long>