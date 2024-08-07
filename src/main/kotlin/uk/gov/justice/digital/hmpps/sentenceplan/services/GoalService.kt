package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepActorEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
  private val planRepository: PlanRepository,
) {

  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val planEntity = planRepository.findByUuid(planUuid)
      ?: throw Exception("A Plan with this UUID was not found: $planUuid")

    val areaOfNeedEntity = areaOfNeedRepository.findByNameIgnoreCase(goal.areaOfNeed)
      ?: throw Exception("An Area of Need with this name was not found: ${goal.areaOfNeed}")

    var relatedAreasOfNeedEntity: List<AreaOfNeedEntity> = emptyList()

    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedEntity = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedEntity.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }

    val highestGoalOrder = planEntity.goals.maxByOrNull { g -> g.goalOrder }?.goalOrder ?: 0

    val goalEntity = GoalEntity(
      title = goal.title,
      areaOfNeed = areaOfNeedEntity,
      targetDate = goal.targetDate,
      plan = planEntity,
      relatedAreasOfNeed = relatedAreasOfNeedEntity,
      goalOrder = highestGoalOrder + 1,
    )
    val savedGoalEntity = goalRepository.save(goalEntity)

    return savedGoalEntity
  }

  @Transactional
  fun updateGoalByUuid(goalUuid: UUID, goal: Goal): GoalEntity {
    // get existing GoalEntity by UUID
    val goalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal is not found: $goalUuid")

    goalEntity.title = goal.title
    goalEntity.targetDate = goal.targetDate

    // get new related objects from DB using the values in Goal (related areas of need)
    var relatedAreasOfNeedEntity: List<AreaOfNeedEntity> = emptyList()

    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedEntity = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedEntity.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }

    // update the GoalEntity object to use the new related areas of need, title, targetDate
    goalEntity.relatedAreasOfNeed = relatedAreasOfNeedEntity

    // save it
    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun createNewSteps(goalUuid: UUID, steps: List<Step>): GoalEntity {
    val goal: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal is not found: $goalUuid")

    val stepEntityList = ArrayList<StepEntity>()
    steps.forEach { step ->
      val stepEntity = StepEntity(
        description = step.description,
        status = step.status,
        goal = goal,
      )

      val stepActorEntityList = ArrayList<StepActorEntity>()
      step.actor.forEach {
        val stepActorEntity = StepActorEntity(
          step = stepEntity,
          actor = it.actor,
          actorOptionId = it.actorOptionId,
        )
        stepActorEntityList.add(stepActorEntity)
      }

      stepEntity.actors = stepActorEntityList
      stepEntityList.add(stepEntity)
    }
    goal.steps = stepEntityList
    return goalRepository.save(goal)
  }

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }

  @Transactional
  fun deleteGoal(goalUuid: UUID): Unit? {
    val goalEntity: GoalEntity? = goalRepository.findByUuid(goalUuid)
    return goalEntity?.let { goalRepository.delete(it) }
  }
}
