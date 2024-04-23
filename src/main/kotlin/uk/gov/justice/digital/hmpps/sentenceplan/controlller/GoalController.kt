package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.service.GoalService
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
    @RequestBody step: StepEntity,
  ): StepEntity {
    step.relatedGoalId = goalId
    return service.createNewStep(step)
  }
}
