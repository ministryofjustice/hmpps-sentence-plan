package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
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

  @OneToMany(mappedBy = "areaOfNeed", fetch = FetchType.LAZY)
  @JsonIgnore
  val goals: List<GoalEntity>?,
)

@Repository
interface AreaOfNeedRepository : JpaRepository<AreaOfNeedEntity, Long> {

  fun findByNameIgnoreCase(name: String): AreaOfNeedEntity?

  @Query("select aon from AreaOfNeed aon where aon.name in :relatedAreasOfNeedNames")
  fun findAllByNames(@Param("relatedAreasOfNeedNames") relatedAreasOfNeedNames: List<String>): List<AreaOfNeedEntity>?
}
