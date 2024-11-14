package uk.gov.justice.digital.hmpps.sentenceplan.data

import java.time.LocalDateTime

data class Note(
  val noteObject: String,
  val note: String,
  val additionalNote: String?,
  val noteType: String,
  val goalTitle: String?,
  val goalUuid: String?,
  val goalStatus: String?,
  val createdDate: LocalDateTime,
  val createdBy: String,
)
