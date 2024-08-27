package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(name = "PlanAgreementNote")
@Table(name = "plan_agreement_notes")
class PlanAgreementNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "plan_uuid")
  var planUuid: UUID,

  @Column(name = "agreement_status")
  @Enumerated(EnumType.STRING)
  var agreementStatus: PlanStatus,

  @Column(name = "agreement_status_note")
  var agreementStatusNote: String,

  @Column(name = "optional_note")
  var optionalNote: String,

  @Column(name = "practitioner_name")
  var practitionerName: String,

  @Column(name = "person_name")
  var personName: String,

  @Column(name = "creation_date")
  val creationDate: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
)

interface PlanAgreementNoteRepository : JpaRepository<PlanAgreementNoteEntity, Long>
