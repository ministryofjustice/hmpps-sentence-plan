package uk.gov.justice.digital.hmpps.sentenceplan.migrator.aap.commands

interface Resolvable {
  fun resolve(commands: List<Requestable>): Requestable
}
