package uk.gov.justice.digital.hmpps.sentenceplan.data

import java.time.LocalDateTime

data class Note(
  val type: String,
  val note: String,
  val noteType: String,
  val createdDate: LocalDateTime,
  val createdBy: Long,
)

// goal title
// goal uuid