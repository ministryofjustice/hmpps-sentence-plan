package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorsEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val stepRepository: StepRepository,
  private val stepActorRepository: StepActorRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
) {

  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  fun getGoalsByPlanUuid(planUuid: UUID): List<GoalEntity> = goalRepository.findByPlanUuid(planUuid)

  fun getGoalsByAreaOfNeed(areaOfNeedName: String) = goalRepository.findByAreaOfNeed(areaOfNeedName)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val areaOfNeedUuid = areaOfNeedRepository.findByName(goal.areaOfNeed).uuid

    val goalEntity = GoalEntity(
      title = goal.title,
      areaOfNeedUuid = areaOfNeedUuid,
      targetDate = goal.targetDate,
      goalOrder = -1,
      planUuid = planUuid,
    )
    val savedGoalEntity = goalRepository.save(goalEntity)

    goal.relatedAreasOfNeed.forEach {
      areaOfNeedRepository.saveRelatedAreaOfNeed(savedGoalEntity.uuid, it)
    }
    return goalEntity
  }

  @Transactional
  fun createNewSteps(goalUuid: UUID, steps: List<Step>): List<StepEntity> {
    val stepEntityList = ArrayList<StepEntity>()
    steps.forEach { step ->
      val stepEntity = StepEntity(
        relatedGoalUuid = goalUuid,
        description = step.description,
        status = step.status,
      )
      val savedStep = stepRepository.save(stepEntity)
      val stepActorEntityList = ArrayList<StepActorsEntity>()
      step.actor.forEach {
        val stepActorsEntity = StepActorsEntity(
          stepUuid = savedStep.uuid,
          actor = it.actor,
          actorOptionId = it.actorOptionId,
        )
        stepActorEntityList.add(stepActorsEntity)
        stepActorRepository.saveAll(stepActorEntityList)
        stepEntityList.add(savedStep)
      }
    }
    return stepEntityList
  }

  fun getAllGoals(): List<GoalEntity> = goalRepository.findAll()

  fun getAllGoalSteps(goalUuid: UUID): List<StepEntity> = stepRepository.findByRelatedGoalUuid(goalUuid)

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }
}
