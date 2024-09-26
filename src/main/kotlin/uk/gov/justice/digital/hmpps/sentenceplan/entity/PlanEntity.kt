package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "plan")
class PlanEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  @JsonIgnore
  var id: Long? = null,

  @Column(name = "published_state")
  @Enumerated(EnumType.STRING)
  var publishedState: PublishState = PublishState.UNPUBLISHED,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID = UUID.randomUUID(),

  @CreatedDate
  @Column(name = "created_date")
  var createdDate: Instant? = Instant.now(),

  @CreatedBy
  @ManyToOne
  @JoinColumn(name = "created_by_id")
  var createdBy: PractitionerEntity? = null,

  @LastModifiedDate
  @Column(name = "last_updated_date", nullable = false)
  var lastUpdatedDate: Instant? = Instant.now(),

  @LastModifiedBy
  @ManyToOne
  @JoinColumn(name = "last_updated_by_id")
  var lastUpdatedBy: PractitionerEntity? = null,

  @OneToOne(mappedBy = "plan")
  var currentVersion: PlanVersionEntity? = null,
)

enum class PublishState {
  UNPUBLISHED,
  PUBLISHED,
  ARCHIVED,
}
