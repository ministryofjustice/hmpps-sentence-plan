package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import java.util.UUID

@RestController
@RequestMapping("/goals")
class GoalController(private val service: GoalService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewGoal(
    @RequestBody goal: GoalEntity,
  ): GoalEntity {
    return service.createNewGoal(goal)
  }

  @PostMapping("/{goalId}/steps")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewStep(
    @PathVariable goalId: UUID,
    @RequestBody steps: List<StepEntity>,
  ): List<StepEntity> {
    return service.createNewStep(steps, goalId)
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoals(): List<GoalEntity> {
    return service.getAllGoals()
  }

  @GetMapping("/{goalId}/steps")
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoalSteps(
    @PathVariable goalId: UUID,
  ): List<StepEntity> {
    return service.getAllGoalSteps(goalId)
  }

  @PostMapping("/order")
  @ResponseStatus(HttpStatus.CREATED)
  fun updateGoalsOrder(
    @RequestBody goalsOrder: List<GoalOrder>,
  ) {
    return service.updateGoalsOrder(goalsOrder)
  }
}
