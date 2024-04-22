package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.google.gson.annotations.SerializedName
import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

@Entity(name = "RefData")
@Table(name = "ref_data")
class RefDataEntity(
  @Id
  @SerializedName("id") val id: Int,

  @Column(name = "reference_data")
  @Type(JsonType::class)
  @SerializedName("refData") val refData: String,
)

interface ReferenceDataRepository : JpaRepository<RefDataEntity, Int> {
  override fun findById(id: Int): Optional<RefDataEntity>
}
