package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalStatus
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import java.util.UUID

private const val ONE_ROW_DELETED = 1

@RestController
@RequestMapping("/goals")
class GoalController(private val service: GoalService) {

  @GetMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE', 'ROLE_SENTENCE_PLAN_READ')")
  fun getGoal(
    @PathVariable goalUuid: UUID,
  ) = service.getGoalByUuid(goalUuid)

  @PutMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun replaceGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ) = service.replaceGoalByUuid(goalUuid, goal)

  @DeleteMapping("/{goalUuid}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteGoal(
    @PathVariable goalUuid: UUID,
  ) {
    if (service.deleteGoalByUuid(goalUuid) != ONE_ROW_DELETED) {
      throw NotFoundException("No goal found for $goalUuid")
    }
  }

  @PostMapping("/{goalUuid}/achieve")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun achieveGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody achieveGoal: Goal,
  ) = service.achieveGoal(goalUuid, achieveGoal.note)

  @PostMapping("/{goalUuid}/remove")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun removeGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody removeGoal: Goal,
  ) = service.removeGoal(goalUuid, removeGoal.note)

  @PostMapping("/{goalUuid}/readd")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun reAddGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody reAddGoal: Goal,
  ): GoalEntity {
    reAddGoal.status = if (reAddGoal.targetDate.isNullOrEmpty()) GoalStatus.FUTURE else GoalStatus.ACTIVE
    return service.reAddGoal(goalUuid, reAddGoal)
  }

  @PostMapping("/{goalUuid}/steps")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewSteps(
    @PathVariable goalUuid: UUID,
    @RequestBody steps: List<Step>,
  ) = service.addStepsToGoal(goalUuid, Goal(steps = steps))

  @GetMapping("/{goalUuid}/steps")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE', 'ROLE_SENTENCE_PLAN_READ')")
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoalSteps(
    @PathVariable goalUuid: UUID,
  ) = service.getGoalByUuid(goalUuid).steps

  @PutMapping("/{goalUuid}/steps")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.OK)
  fun updateStep(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ) = service.addStepsToGoal(goalUuid, goal, true)

  @PostMapping("/order")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun updateGoalsOrder(
    @RequestBody goalsOrder: List<GoalOrder>,
  ) = service.updateGoalsOrder(goalsOrder)
}
