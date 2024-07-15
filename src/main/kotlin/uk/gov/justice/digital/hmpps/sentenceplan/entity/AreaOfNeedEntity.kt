package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
)

@Repository
interface AreaOfNeedRepository : JpaRepository<AreaOfNeedEntity, Long> {

  fun findByUuid(uuid: UUID): AreaOfNeedEntity

  fun findByName(name: String): AreaOfNeedEntity

  @Modifying
  @Transactional
  @Query("insert into related_area_of_need(goal_uuid, area_of_need_uuid) select :goalUuid, aon.uuid from area_of_need aon where aon.name = :relatedAreaOfNeedName", nativeQuery = true)
  fun saveRelatedAreaOfNeed(
    @Param("goalUuid") goalUuid: UUID,
    @Param("relatedAreaOfNeedName") relatedAreaOfNeedName: String,
  )

  @Query(
    "select aon.id, aon.uuid, aon.name from area_of_need aon inner join related_area_of_need raon " +
      "on aon.uuid = raon.area_of_need_uuid where raon.goal_uuid = :goalUuid",
    nativeQuery = true,
  )
  fun findRelatedAreasOfNeedByGoal(@Param("goalUuid") goalUuid: UUID): Set<AreaOfNeedEntity>
}
