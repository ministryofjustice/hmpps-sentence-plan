package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.google.gson.annotations.SerializedName


data class Steps (

  @SerializedName("id"     ) var id     : Int?     = null,
  @SerializedName("Name"   ) var Name   : String?  = null,
  @SerializedName("Active" ) var Active : Boolean? = null

)