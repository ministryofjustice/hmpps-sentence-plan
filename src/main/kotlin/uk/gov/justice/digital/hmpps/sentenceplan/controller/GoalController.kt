package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.GoalOrder
import uk.gov.justice.digital.hmpps.sentenceplan.data.Step
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import java.util.UUID

@RestController
@RequestMapping("/goals")
class GoalController(private val service: GoalService) {

  @GetMapping("/{goalUuid}")
  fun getGoal(
    @PathVariable goalUuid: UUID,
  ): GoalEntity {
    try {
      return service.getGoalByUuid(goalUuid)
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }

  @PatchMapping("/{goalUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun updateGoal(
    @PathVariable goalUuid: UUID,
    @RequestBody goal: Goal,
  ): GoalEntity {
    try {
      return service.updateGoalByUuid(goalUuid, goal)
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }

  @DeleteMapping("/{goalUuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteGoal(
    @PathVariable goalUuid: UUID,
  ) {
    try {
      service.deleteGoal(goalUuid)
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }

  @PostMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewSteps(
    @PathVariable goalUuid: UUID,
    @RequestBody steps: List<Step>,
  ): List<StepEntity> {
    try {
      val goal: GoalEntity = service.createNewSteps(goalUuid, steps)
      return goal.steps
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }

  @GetMapping("/{goalUuid}/steps")
  @ResponseStatus(HttpStatus.OK)
  fun getAllGoalSteps(
    @PathVariable goalUuid: UUID,
  ): List<StepEntity> {
    try {
      val goal: GoalEntity = service.getGoalByUuid(goalUuid)
      return goal.steps
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }

  @PostMapping("/order")
  @ResponseStatus(HttpStatus.CREATED)
  fun updateGoalsOrder(
    @RequestBody goalsOrder: List<GoalOrder>,
  ) {
    try {
      return service.updateGoalsOrder(goalsOrder)
    } catch (e: EmptyResultDataAccessException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found", e)
    }
  }
}
