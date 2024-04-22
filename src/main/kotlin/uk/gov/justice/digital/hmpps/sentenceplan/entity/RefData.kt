package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName


data class RefData (

  @JsonProperty("AreasOfNeed")
  val areasOfNeed: List<AreasOfNeed>,

)