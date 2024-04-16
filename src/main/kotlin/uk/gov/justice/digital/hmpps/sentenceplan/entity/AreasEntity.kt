package uk.gov.justice.digital.hmpps.sentenceplan.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Entity(name = "Areas")
@Table(name = "areas")
class AreasEntity(
  @Id
  val id: Int,
  var name: String,
  var active: Boolean,
)

interface AreasRepository : JpaRepository<AreasEntity, Int> {
  override fun findById(id: Int): Optional<AreasEntity>
}
