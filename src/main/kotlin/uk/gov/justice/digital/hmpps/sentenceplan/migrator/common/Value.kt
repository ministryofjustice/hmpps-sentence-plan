package uk.gov.justice.digital.hmpps.sentenceplan.migrator.common

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = SingleValue::class, name = "Single"),
  JsonSubTypes.Type(value = MultiValue::class, name = "Multi"),
)
sealed interface Value

data class SingleValue(val value: String) : Value

data class MultiValue(val values: List<String>) : Value {
  companion object {
    fun of(vararg values: String) = MultiValue(values.toList())
  }
}
