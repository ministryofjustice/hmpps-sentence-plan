package uk.gov.justice.digital.hmpps.sentenceplan.services

import jakarta.validation.ValidationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalStatusUpdate
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalNoteType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val areaOfNeedRepository: AreaOfNeedRepository,
  private val stepRepository: StepRepository,
  private val planRepository: PlanRepository,
  private val versionService: VersionService,
) {
  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val planVersionEntity: PlanVersionEntity

    try {
      planVersionEntity = planRepository.findByUuid(planUuid).currentVersion!!
    } catch (e: EmptyResultDataAccessException) {
      throw Exception("A Plan with this UUID was not found: $planUuid")
    }

    require(goal.areaOfNeed != null && goal.title != null)

    val areaOfNeedEntity: AreaOfNeedEntity

    try {
      areaOfNeedEntity = areaOfNeedRepository.findByNameIgnoreCase(goal.areaOfNeed)
    } catch (e: EmptyResultDataAccessException) {
      throw Exception("An Area of Need with this name was not found: ${goal.areaOfNeed}")
    }

    val relatedAreasOfNeedEntity = getAreasOfNeedByNames(goal)

    val highestGoalOrder = planVersionEntity.goals.maxByOrNull { g -> g.goalOrder }?.goalOrder ?: 0

    val currentPlanVersion = versionService.conditionallyCreateNewPlanVersion(planVersionEntity)

    val goalEntity = GoalEntity(
      title = goal.title,
      areaOfNeed = areaOfNeedEntity,
      targetDate = goal.targetDate?.let { LocalDate.parse(it) },
      status = if (goal.targetDate != null) GoalStatus.ACTIVE else GoalStatus.FUTURE,
      statusDate = LocalDateTime.now(),
      planVersion = currentPlanVersion,
      relatedAreasOfNeed = relatedAreasOfNeedEntity.toMutableSet(),
      goalOrder = highestGoalOrder + 1,
    )
    val savedGoalEntity = goalRepository.save(goalEntity)

    return savedGoalEntity
  }

  private fun getAreasOfNeedByNames(goal: Goal): List<AreaOfNeedEntity> {
    var relatedAreasOfNeedList: List<AreaOfNeedEntity> = emptyList()
    if (goal.relatedAreasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedList = areaOfNeedRepository.findAllByNames(goal.relatedAreasOfNeed)
        ?: throw Exception("One or more of the Related Areas of Need was not found: ${goal.relatedAreasOfNeed}")

      // findAllByNames doesn't throw an exception if a subset of goal.relatedAreasOfNeed is not found, so we
      // do a hard check on the count of returned items here
      if (goal.relatedAreasOfNeed.size != relatedAreasOfNeedList.size) {
        throw Exception("One or more of the Related Areas of Need was not found")
      }
    }
    return relatedAreasOfNeedList
  }

  /**
   * This function expects a full Goal object - it cannot be used for updating individual fields in
   * a Goal object identified by the UUID unless the rest of the goal object is passed in.
   * In particular the GoalEntity.merge function will remove all Related Areas of Need from a GoalEntity
   * if there are no Related Areas of Need in the Goal parameter received here.
   */
  @Transactional
  fun replaceGoalByUuid(goalUuid: UUID, goal: Goal): GoalEntity {
    val goalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    validateGoalFields(goal)

    val relatedAreasOfNeedList: List<AreaOfNeedEntity> = getAreasOfNeedByNames(goal)
    goalEntity.merge(goal, relatedAreasOfNeedList)

    versionService.conditionallyCreateNewPlanVersion(goalEntity.planVersion)

    return goalRepository.save(goalEntity)
  }

  private fun validateGoalFields(goal: Goal) {
    val requiredFields = listOf(goal.title, goal.areaOfNeed)
    if (requiredFields.any { it == null }) {
      throw ValidationException("One or more required fields are null")
    }
  }

  @Transactional
  fun addStepsToGoal(goalUuid: UUID, goal: Goal, replaceExistingSteps: Boolean = false): List<StepEntity> {
    var goalEntity: GoalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw Exception("This Goal was not found: $goalUuid")

    if (goal.steps.isEmpty() && goal.note.isNullOrEmpty()) {
      throw IllegalArgumentException("A Step or Note must be provided")
    }

    if (goal.steps.isNotEmpty()) {
      requireStepsAreValid(goal.steps)
    }

    val planVersion = goalEntity.planVersion
    versionService.conditionallyCreateNewPlanVersion(planVersion)

    // the planversion has changed, so refetch the goal to make sure we have the right version tree
    goalEntity = goalRepository.findByUuid(goalUuid)!!

    if (goal.steps.isNotEmpty()) {
      if (replaceExistingSteps) {
        stepRepository.deleteAll(goalEntity.steps)
      }
      goalEntity.steps = createStepEntitiesFromSteps(goalEntity, goal.steps)
    }

    goal.note?.takeIf { it.isNotEmpty() }?.let { note ->
      goalEntity.notes.add(
        GoalNoteEntity(
          note = note,
          type = GoalNoteType.PROGRESS,
          goal = goalEntity,
        ),
      )
    }

    val savedGoal: GoalEntity = goalRepository.save(goalEntity)
    return savedGoal.steps
  }

  private fun requireStepsAreValid(steps: List<Step>) {
    steps.forEach {
      require(it.description.isNotEmpty() && it.actor.isNotEmpty()) {
        "All Steps must contain all the required information"
      }
    }
  }

  private fun createStepEntitiesFromSteps(
    goal: GoalEntity,
    steps: List<Step>,
  ): List<StepEntity> = steps.map {
    StepEntity(
      description = it.description,
      status = it.status,
      goal = goal,
      actor = it.actor,
    )
  }

  @Transactional
  fun updateGoalsOrder(goalsOrder: List<GoalOrder>) {
    for (goal in goalsOrder) {
      goal.goalOrder?.let { goalRepository.updateGoalOrder(it, goal.goalUuid) }
    }
  }

  @Transactional
  fun deleteGoalByUuid(goalUuid: UUID): Int {
    goalRepository.findByUuid(goalUuid)?.let { versionService.conditionallyCreateNewPlanVersion(it.planVersion) }
    return goalRepository.deleteByUuid(goalUuid)
  }

  @Transactional
  fun updateGoalStatus(goalUuid: UUID, goalStatusUpdate: GoalStatusUpdate) {
    // 1. if we have a note value, create a new note of the correct type
    // 2. update the current goal status

    val goalEntity = goalRepository.getGoalByUuid(goalUuid)

    val goalNoteEntity = GoalNoteEntity(note = goalStatusUpdate.note, goal = goalEntity).apply {
      type = when (goalStatusUpdate.status) {
        GoalStatus.REMOVED -> GoalNoteType.REMOVED
        GoalStatus.ACHIEVED -> GoalNoteType.ACHIEVED
        else -> GoalNoteType.PROGRESS
      }
    }

    goalEntity.notes.add(goalNoteEntity)

    goalEntity.status = goalStatusUpdate.status
    goalEntity.statusDate = LocalDateTime.now()
  }
}
