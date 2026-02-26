package uk.gov.justice.digital.hmpps.sentenceplan.migrator.commands

interface Resolvable {
  fun resolve(commands: List<Requestable>)
}