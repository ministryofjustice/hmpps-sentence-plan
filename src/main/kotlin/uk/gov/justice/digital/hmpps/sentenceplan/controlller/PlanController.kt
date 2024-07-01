package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.*

@RestController
@RequestMapping("/plans")
class PlanController(private val service: PlanService) {

  @GetMapping("/{planUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable planUuid: UUID,
  ): PlanEntity {
    return service.getPlanByUuid(planUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No Plan found for $planUuid")
  }

  @GetMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanGoals(
    @PathVariable planUuid: UUID,
  ): List<GoalEntity> {
    return service.getGoalsByPlanUuid(planUuid)
  }

  @PostMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewGoal(
    @PathVariable planUuid: UUID,
    @RequestBody goal: GoalEntity,
  ): GoalEntity {
    return service.createNewGoal(planUuid, goal)
  }
}
