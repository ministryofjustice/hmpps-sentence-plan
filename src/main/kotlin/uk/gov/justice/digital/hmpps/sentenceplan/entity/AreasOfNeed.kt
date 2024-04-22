package uk.gov.justice.digital.hmpps.sentenceplan.entity

import com.google.gson.annotations.SerializedName


data class AreasOfNeed (

  @SerializedName("id"     ) var id     : Int?             = null,
  @SerializedName("Name"   ) var Name   : String?          = null,
  @SerializedName("active" ) var active : Boolean?         = null,
  @SerializedName("Goals"  ) var Goals  : ArrayList<Goals> = arrayListOf()

)