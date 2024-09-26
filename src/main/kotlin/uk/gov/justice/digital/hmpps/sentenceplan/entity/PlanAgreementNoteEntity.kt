package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

@Entity(name = "PlanAgreementNote")
@Table(name = "plan_agreement_notes")
class PlanAgreementNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_version_id", referencedColumnName = "id")
  @JsonIgnore
  val planVersion: PlanVersionEntity?,

  @Column(name = "optional_note")
  var optionalNote: String,

  @Column(name = "agreement_status")
  @Enumerated(EnumType.STRING)
  var agreementStatus: PlanAgreementStatus,

  @Column(name = "agreement_status_note")
  var agreementStatusNote: String,

  @Column(name = "practitioner_name")
  var practitionerName: String,

  @Column(name = "person_name")
  var personName: String,

  @Column(name = "created_date")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,
)

interface PlanAgreementNoteRepository : JpaRepository<PlanAgreementNoteEntity, Long>
