package uk.gov.justice.digital.hmpps.sentenceplan.data

import java.time.LocalDateTime

data class Note(
  val noteObject: String,
  val note: String,
  val additionalNote: String,
  val noteStatus: String,
  val goalTitle: String,
  val goalUuid: String,
  val createdDate: LocalDateTime,
  val createdBy: String,
)
