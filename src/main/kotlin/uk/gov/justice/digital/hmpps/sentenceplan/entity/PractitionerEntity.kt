package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Entity(name = "Practitioner")
@Table(name = "practitioner")
class PractitionerEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "uuid")
  val uuid: String,

  @Column(name = "username")
  val username: String,
)

interface PractitionerRepository : JpaRepository<PractitionerEntity, Long> {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun findByUsername(username: String): PractitionerEntity

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  override fun <S : PractitionerEntity> save(entity: S): S
}
