package uk.gov.justice.digital.hmpps.sentenceplan.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.data.Agreement
import uk.gov.justice.digital.hmpps.sentenceplan.data.Goal
import uk.gov.justice.digital.hmpps.sentenceplan.services.GoalService
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import uk.gov.justice.digital.hmpps.sentenceplan.services.VersionService
import java.util.UUID

@RestController
@RequestMapping("/plans")
class PlanController(
  private val planService: PlanService,
  private val goalService: GoalService,
  private val versionService: VersionService,
) {

  @GetMapping("/{planUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable planUuid: UUID,
  ) = planService.getPlanVersionByPlanUuid(planUuid)

  @GetMapping("/{planUuid}/notes")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanAndGoalNotes(
    @PathVariable planUuid: UUID,
  ) = planService.getPlanAndGoalNotes(planUuid)

  @GetMapping("/{planUuid}/version/{planVersionNumber}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanVersion(
    @PathVariable planUuid: UUID,
    @PathVariable planVersionNumber: Int,
  ) = planService.getPlanVersionByPlanUuidAndPlanVersion(planUuid, planVersionNumber)

  @GetMapping("/version/{planVersionUuid}")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanVersionByVersionUuid(
    @PathVariable planVersionUuid: UUID,
  ) = versionService.getPlanVersionByVersionUuid(planVersionUuid)

  @GetMapping("/{planUuid}/goals")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanGoals(
    @PathVariable planUuid: UUID,
  ) = planService.getPlanVersionByPlanUuid(planUuid)
    .goals.partition { it.targetDate != null }
    .let { (now, future) -> mapOf("now" to now, "future" to future) }

  @PostMapping("/{planUuid}/goals")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createNewGoal(
    @PathVariable planUuid: UUID,
    @RequestBody goal: Goal,
  ) = goalService.createNewGoal(planUuid, goal)

  @PostMapping("/{planUuid}/agree")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun agreePlanVersion(
    @PathVariable planUuid: UUID,
    @RequestBody agreement: Agreement,
  ) = planService.agreeLatestPlanVersion(planUuid, agreement)

  @PutMapping("associate/{planUuid}/{crn}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun associateCrnWithPlan(
    @PathVariable planUuid: UUID,
    @PathVariable crn: String,
  ) = planService.associate(planUuid, crn)

  @GetMapping("/crn/{crn}")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE', 'ROLE_SENTENCE_PLAN_READ')")
  @ResponseStatus(HttpStatus.OK)
  fun getPlanByCrn(
    @PathVariable crn: String,
  ) = planService.getPlansByCrn(crn)
}
