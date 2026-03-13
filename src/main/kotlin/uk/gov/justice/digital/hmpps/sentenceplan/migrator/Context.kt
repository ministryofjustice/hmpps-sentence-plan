package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity

data class Context(
  val plan: PlanEntity,
  val assessmentUuid: String,
  val goalsCollectionUuid: String,
  val goals: MutableSet<String> = mutableSetOf(),
  val planAgreementsCollectionUuid: String,
  val planAgreements: MutableSet<String> = mutableSetOf(),
  var previousVersion: Int = 0,
)
