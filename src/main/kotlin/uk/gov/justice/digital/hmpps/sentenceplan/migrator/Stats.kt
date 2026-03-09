package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import java.time.Duration
import java.time.LocalDateTime

class Stats() {
  companion object {
    private lateinit var started: LocalDateTime
    var numberOfEvents = 0
    var numberOfVersions = 0
    fun start() {
      started = LocalDateTime.now()
    }

    fun getDuration(): Duration = Duration.between(started, LocalDateTime.now())
  }
}
