package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity(name = "GoalNote")
@Table(name = "goal_notes")
@EntityListeners(AuditingEntityListener::class)
class GoalNoteEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  var id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "goal_id", referencedColumnName = "id")
  @JsonIgnore
  var goal: GoalEntity? = null,

  @Column(name = "note")
  var note: String,

  @Column(name = "note_type")
  @Enumerated(EnumType.STRING)
  var type: GoalNoteType = GoalNoteType.PROGRESS,

  @CreatedDate
  @Column(name = "created_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime = LocalDateTime.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  @JsonIgnore
  var createdBy: PractitionerEntity? = null,
) {
  @JsonProperty("practitionerName")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  fun getPractitionerName(): String? {
    return createdBy?.username
  }
}

enum class GoalNoteType {
  PROGRESS,
  REMOVED,
  ACHIEVED,
}
