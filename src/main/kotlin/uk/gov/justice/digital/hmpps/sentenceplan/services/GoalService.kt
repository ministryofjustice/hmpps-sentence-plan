package uk.gov.justice.digital.hmpps.sentenceplan.services

import jakarta.validation.ValidationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
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
  private val planVersionRepository: PlanVersionRepository,
) {
  fun getGoalByUuid(goalUuid: UUID): GoalEntity? = goalRepository.findByUuid(goalUuid)

  @Transactional
  fun createNewGoal(planUuid: UUID, goal: Goal): GoalEntity {
    val planVersionEntity: PlanVersionEntity

    planVersionEntity = planRepository.getByUuid(planUuid).currentVersion!!

    require(goal.areaOfNeed != null && goal.title != null)

    val areaOfNeedEntity: AreaOfNeedEntity

    try {
      areaOfNeedEntity = areaOfNeedRepository.findByNameIgnoreCase(goal.areaOfNeed)
    } catch (e: EmptyResultDataAccessException) {
      throw NotFoundException("An Area of Need with this name was not found: ${goal.areaOfNeed}")
    }

    val relatedAreasOfNeedEntity = getAreasOfNeedByNames(goal.relatedAreasOfNeed)

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

  private fun getAreasOfNeedByNames(areasOfNeed: List<String>): List<AreaOfNeedEntity> {
    var relatedAreasOfNeedList: List<AreaOfNeedEntity> = emptyList()
    if (areasOfNeed.isNotEmpty()) {
      relatedAreasOfNeedList = areaOfNeedRepository.findAllByNames(areasOfNeed)
        ?: throw NotFoundException("One or more of the Related Areas of Need was not found: $areasOfNeed")

      // findAllByNames doesn't throw an exception if a subset of goal.relatedAreasOfNeed is not found, so we
      // do a hard check on the count of returned items here
      if (areasOfNeed.size != relatedAreasOfNeedList.size) {
        throw NotFoundException("One or more of the Related Areas of Need was not found")
      }
    }
    return relatedAreasOfNeedList
  }

  /**
   * This function expects a full Goal object - it cannot be used for updating individual fields in
   * a Goal object identified by the UUID unless the rest of the goal object is passed in.
   * In particular the GoalEntity.merge function will intentionally remove all Related Areas of Need from a GoalEntity
   * if there are no Related Areas of Need in the Goal parameter received here.
   */
  @Transactional
  fun replaceGoalByUuid(goalUuid: UUID, goal: Goal): GoalEntity {
    val goalEntity = goalRepository.findByUuid(goalUuid)
      ?: throw NotFoundException("This Goal was not found: $goalUuid")

    validateGoalFields(goal)

    val relatedAreasOfNeedList: List<AreaOfNeedEntity> = getAreasOfNeedByNames(goal.relatedAreasOfNeed)
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
      ?: throw NotFoundException("This Goal was not found: $goalUuid")

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
  fun updateGoalStatus(goalUuid: UUID, updatedGoal: Goal): GoalEntity {
    if (updatedGoal.status == null) {
      throw ValidationException("Updated Goal status must not be null")
    }

    val goalEntity = goalRepository.getGoalByUuid(goalUuid)

    goalEntity.notes.add(
      GoalNoteEntity(note = updatedGoal.note!!, goal = goalEntity, type = GoalNoteType.PROGRESS),
    )
    goalEntity.status = updatedGoal.status!!
    goalEntity.statusDate = LocalDateTime.now()

    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun achieveGoal(goalUuid: UUID, updatedGoal: Goal): GoalEntity {
    if (updatedGoal.status != GoalStatus.ACHIEVED) {
      throw ValidationException("Updated Goal status must be ACHIEVED but was ${updatedGoal.status}")
    }

    val goalEntity = goalRepository.getGoalByUuid(goalUuid)

    goalEntity.notes.add(
      GoalNoteEntity(note = updatedGoal.note!!, goal = goalEntity, type = GoalNoteType.ACHIEVED),
    )
    goalEntity.status = updatedGoal.status!!
    goalEntity.statusDate = LocalDateTime.now()

    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun removeGoal(goalUuid: UUID, updatedGoal: Goal): GoalEntity {
    if (updatedGoal.status != GoalStatus.REMOVED) {
      throw ValidationException("Updated Goal status must be REMOVED but was ${updatedGoal.status}")
    } else if (updatedGoal.note.isNullOrEmpty()) { // note is mandatory for a removed goal
      throw ValidationException("Updated goal note must not be empty")
    }

    val goalEntity = goalRepository.getGoalByUuid(goalUuid)

    goalEntity.notes.add(
      GoalNoteEntity(note = updatedGoal.note, goal = goalEntity, type = GoalNoteType.REMOVED),
    )
    goalEntity.status = updatedGoal.status!!
    goalEntity.statusDate = LocalDateTime.now()

    return goalRepository.save(goalEntity)
  }

  @Transactional
  fun reAddGoal(goalUuid: UUID, updatedGoal: Goal): GoalEntity {
    if (updatedGoal.status != GoalStatus.ACTIVE && updatedGoal.status != GoalStatus.FUTURE) {
      throw ValidationException("Updated Goal status must be ACTIVE or FUTURE but was ${updatedGoal.status}")
    }

    val goalEntity = goalRepository.getGoalByUuid(goalUuid)

    goalEntity.notes.add(
      GoalNoteEntity(note = updatedGoal.note!!, goal = goalEntity, type = GoalNoteType.READDED),
    )

    val planVersionEntity: PlanVersionEntity
    try {
      planVersionEntity = planVersionRepository.findByUuid(goalEntity.planVersion!!.uuid)
    } catch (e: EmptyResultDataAccessException) {
      throw NotFoundException("A Plan with this UUID was not found: $goalEntity.planVersion!!.uuid")
    }

    val highestGoalOrder = planVersionEntity.goals.maxByOrNull { g -> g.goalOrder }?.goalOrder ?: 0
    goalEntity.goalOrder = highestGoalOrder + 1

    // also need to set the new targetDate
    if (updatedGoal.targetDate != null) {
      goalEntity.targetDate = LocalDate.parse(updatedGoal.targetDate)
      goalEntity.status = GoalStatus.ACTIVE
      goalEntity.statusDate = LocalDateTime.now()
    } else {
      goalEntity.targetDate = null
      goalEntity.status = GoalStatus.FUTURE
      goalEntity.statusDate = LocalDateTime.now()
    }

    return goalRepository.save(goalEntity)
  }
}
