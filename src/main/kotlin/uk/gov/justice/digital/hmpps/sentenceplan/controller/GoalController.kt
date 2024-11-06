package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalStatusUpdate
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
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
  ): GoalEntity {
    return service.getGoalByUuid(goalUuid) ?: throw NoResourceFoundException(
      HttpMethod.GET,
      "No goal found for $goalUuid",
    )
  }

  @PutMapping("/{goalUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun replaceGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ): GoalEntity {
    return service.replaceGoalByUuid(goalUuid, goal)
  }

  @DeleteMapping("/{goalUuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteGoal(
    @PathVariable goalUuid: UUID,
  ) {
    if (service.deleteGoalByUuid(goalUuid) != ONE_ROW_DELETED) {
      throw NoResourceFoundException(HttpMethod.DELETE, "No goal found for $goalUuid")
    }
  }

  @PatchMapping("/{goalUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun updateGoalStatus(
    @PathVariable goalUuid: UUID,
    @RequestBody goalStatusUpdate: GoalStatusUpdate,
  ) {
    return service.updateGoalStatus(goalUuid, goalStatusUpdate)
  }

  @PostMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewSteps(
    @PathVariable goalUuid: UUID,
    @RequestBody steps: List<Step>,
  ): List<StepEntity> {
    return service.addStepsToGoal(goalUuid, Goal(steps = steps))
  }

  @GetMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoalSteps(
    @PathVariable goalUuid: UUID,
  ): List<StepEntity> {
    val goal: GoalEntity = service.getGoalByUuid(goalUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No goal found for $goalUuid")
    return goal.steps
  }

  @PutMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.OK)
  fun updateStep(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ): List<StepEntity>? {
    return service.addStepsToGoal(goalUuid, goal, true)
  }

  @PostMapping("/order")
  @ResponseStatus(HttpStatus.CREATED)
  fun updateGoalsOrder(
    @RequestBody goalsOrder: List<GoalOrder>,
  ) {
    return service.updateGoalsOrder(goalsOrder)
  }
}
