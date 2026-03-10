package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity

data class Context(
  val plan: PlanEntity,
  val assessmentUuid: String,
  val goalsCollectionUuid: String,
  val goals: MutableSet<String> = mutableSetOf(),
  val planAgreementsCollectionUuid: String,
  val planAgreements: MutableSet<String> = mutableSetOf(),
  var previousPlanVersion: String? = null,
  var previousGoals: Set<GoalEntity> = setOf(),
  var stepCollectionUuids: MutableMap<String, String> =  mutableMapOf(),
  var notesCollectionUuids: MutableMap<String, String> = mutableMapOf(),
  var createdGoalUuids: MutableMap<String, String> = mutableMapOf(),
  var createdStepsUuids: MutableMap<String, String> = mutableMapOf(),
  var createdNotesUuids: MutableMap<String, String> = mutableMapOf(),
  var createdAgreementNotesUuids: MutableMap<String, String> = mutableMapOf(),
)
