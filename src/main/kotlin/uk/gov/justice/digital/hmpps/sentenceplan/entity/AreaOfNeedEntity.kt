package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

@Entity(name = "AreaOfNeed")
@Table(name = "area_of_need")
class AreaOfNeedEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: UUID,

  @Column(name = "name")
  var name: String,
)

interface AreaOfNeedRepository : JpaRepository<AreaOfNeedEntity, Long> {
  fun findByUuid(uuid: UUID): AreaOfNeedEntity

  fun findByName(name: String): AreaOfNeedEntity
}
