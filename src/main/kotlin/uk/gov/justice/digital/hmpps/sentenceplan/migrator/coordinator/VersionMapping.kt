package uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator

import java.time.LocalDateTime

data class VersionMapping(val version: Long, val createdAt: LocalDateTime, var event: String)
