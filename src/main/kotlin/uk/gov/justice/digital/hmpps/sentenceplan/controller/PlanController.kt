package uk.gov.justice.digital.hmpps.sentenceplan.controller

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
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.UUID


@RestController
@RequestMapping("/plans")
class PlanController(
  private val planService: PlanService,
  private val goalService: GoalService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createPlan(): PlanEntity {
    return planService.createPlan()
  }

  @GetMapping("/{planUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable planUuid: UUID,
  ): PlanEntity {
    return planService.getPlanByUuid(planUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No Plan found for $planUuid")
  }

  @GetMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanGoals(
    @PathVariable planUuid: UUID,
  ): Map<String, List<GoalEntity>> {
    val plan = planService.getPlanByUuid(planUuid) ?: throw NoResourceFoundException(HttpMethod.GET, "No Plan found for $planUuid")
    val(now, future) = plan.goals.partition { it.targetDate != null }
    return mapOf("now" to now, "future" to future)
  }

  @PostMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewGoal(
    @PathVariable planUuid: UUID,
    @RequestBody goal: Goal,
  ): GoalEntity {
    return goalService.createNewGoal(planUuid, goal)
  }

  @PostMapping("/{planUuid}/agree")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun agreePlan(
    @PathVariable planUuid: UUID,
    @RequestBody agreement: Agreement,
  ): PlanEntity {
    return planService.agreePlan(planUuid, agreement)
  }
}
