package uk.gov.justice.digital.hmpps.sentenceplan.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class GoalOrder(
  @SerializedName("goalUuid") var goalUuid: UUID,
  @SerializedName("galOrder") var goalOrder: Int? = null,
)
