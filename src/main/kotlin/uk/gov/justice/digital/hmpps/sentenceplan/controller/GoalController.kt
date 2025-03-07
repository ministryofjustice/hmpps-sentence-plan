package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.ReAddGoal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import java.util.UUID

private const val ONE_ROW_DELETED = 1

@RestController
@RequestMapping("/goals")
class GoalController(private val service: GoalService) {

  @GetMapping("/{goalUuid}")
  fun getGoal(
    @PathVariable goalUuid: UUID,
  ): GoalEntity = service.getGoalByUuid(goalUuid) ?: throw NoResourceFoundException(
    HttpMethod.GET,
    "No goal found for $goalUuid",
  )

  @PutMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun replaceGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ): GoalEntity = service.replaceGoalByUuid(goalUuid, goal)

  @DeleteMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteGoal(
    @PathVariable goalUuid: UUID,
  ) {
    if (service.deleteGoalByUuid(goalUuid) != ONE_ROW_DELETED) {
      throw NoResourceFoundException(HttpMethod.DELETE, "No goal found for $goalUuid")
    }
  }

  @PostMapping("/{goalUuid}/achieve")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun achieveGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody note: String,
  ): GoalEntity {
    val goal = Goal(status = GoalStatus.ACHIEVED, note = note)
    return service.updateGoalStatus(goalUuid, goal)
  }

  @PostMapping("/{goalUuid}/remove")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun removeGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody note: String,
  ): GoalEntity {
    val goal = Goal(status = GoalStatus.REMOVED, note = note)
    return service.updateGoalStatus(goalUuid, goal)
  }

  @PostMapping("/{goalUuid}/readd")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun reAddGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody reAddGoal: ReAddGoal,
  ): GoalEntity {
    var goalStatus = GoalStatus.FUTURE
    if (!reAddGoal.targetDate.isNullOrEmpty()) {
      goalStatus = GoalStatus.ACTIVE
    }
    val goal = Goal(
      note = reAddGoal.note,
      targetDate = reAddGoal.targetDate,
      status = goalStatus,
    )
    return service.updateGoalStatus(goalUuid, goal)
  }

  @PatchMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun updateGoalStatus(
    @PathVariable goalUuid: UUID,
    @RequestBody goalStatusUpdate: Goal,
  ): GoalEntity = service.updateGoalStatus(goalUuid, goalStatusUpdate)

  @PostMapping("/{goalUuid}/steps")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewSteps(
    @PathVariable goalUuid: UUID,
    @RequestBody steps: List<Step>,
  ): List<StepEntity> = service.addStepsToGoal(goalUuid, Goal(steps = steps))

  @GetMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoalSteps(
    @PathVariable goalUuid: UUID,
  ): List<StepEntity> {
    val goal: GoalEntity = service.getGoalByUuid(goalUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No goal found for $goalUuid")
    return goal.steps
  }

  @PutMapping("/{goalUuid}/steps")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun updateStep(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ): List<StepEntity>? = service.addStepsToGoal(goalUuid, goal, true)

  @PostMapping("/order")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun updateGoalsOrder(
    @RequestBody goalsOrder: List<GoalOrder>,
  ) = service.updateGoalsOrder(goalsOrder)
}
