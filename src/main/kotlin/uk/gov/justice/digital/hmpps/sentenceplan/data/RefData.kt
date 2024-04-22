package uk.gov.justice.digital.hmpps.sentenceplan.data

import com.fasterxml.jackson.annotation.JsonProperty

data class RefData(
  @JsonProperty("AreasOfNeed")
  val areasOfNeed: List<AreasOfNeed>,
)
