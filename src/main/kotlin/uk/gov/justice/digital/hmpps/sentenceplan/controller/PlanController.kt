package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.data.Note
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.sentenceplan.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.UUID

@RestController
@RequestMapping("/plans")
class PlanController(
  private val planService: PlanService,
  private val goalService: GoalService,
) {

  @GetMapping("/{planUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable planUuid: UUID,
  ): PlanVersionEntity {
    try {
      return planService.getPlanVersionByPlanUuid(planUuid)
    } catch (e: NotFoundException) {
      throw NoResourceFoundException(HttpMethod.GET, "Could not find a plan with ID: $planUuid")
    }
  }

  @GetMapping("/{planUuid}/notes")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanAndGoalNotes(
    @PathVariable planUuid: UUID,
  ): List<Note> {
    try {
      return planService.getPlanAndGoalNotes(planUuid)
    } catch (e: EmptyResultDataAccessException) {
      throw NoResourceFoundException(HttpMethod.GET, "Could not find a plan with ID: $planUuid")
    }
  }

  @GetMapping("/{planUuid}/version/{planVersionNumber}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanVersion(
    @PathVariable planUuid: UUID,
    @PathVariable planVersionNumber: Int,
  ): PlanVersionEntity {
    try {
      return planService.getPlanVersionByPlanUuidAndPlanVersion(planUuid, planVersionNumber)
    } catch (e: NotFoundException) {
      throw NoResourceFoundException(HttpMethod.GET, e.message!!)
    }
  }

  @GetMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanGoals(
    @PathVariable planUuid: UUID,
  ): Map<String, List<GoalEntity>> {
    try {
      val plan = planService.getPlanVersionByPlanUuid(planUuid)
      val (now, future) = plan.goals.partition { it.targetDate != null }
      return mapOf("now" to now, "future" to future)
    } catch (e: EmptyResultDataAccessException) {
      throw NoResourceFoundException(HttpMethod.GET, "Could not retrieve the latest version of plan with ID: $planUuid")
    }
  }

  @PostMapping("/{planUuid}/goals")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewGoal(
    @PathVariable planUuid: UUID,
    @RequestBody goal: Goal,
  ): GoalEntity = goalService.createNewGoal(planUuid, goal)

  @PostMapping("/{planUuid}/agree")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun agreePlanVersion(
    @PathVariable planUuid: UUID,
    @RequestBody agreement: Agreement,
  ): PlanVersionEntity {
    try {
      return planService.agreeLatestPlanVersion(planUuid, agreement)
    } catch (e: NotFoundException) {
      throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.message)
    } catch (e: ConflictException) {
      throw ResponseStatusException(HttpStatus.CONFLICT, e.message)
    }
  }
}
