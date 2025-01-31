package uk.gov.justice.digital.hmpps.sentenceplan.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
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
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.sentenceplan.config.ErrorResponse
import uk.gov.justice.digital.hmpps.sentenceplan.data.CreatePlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.data.LockPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.ClonePlanVersionRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CounterSignPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.RestorePlanVersionsRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.RollbackPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SoftDeletePlanVersionsRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.sentenceplan.entity.response.PlanVersionResponse
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService
import java.util.UUID

@RestController
@RequestMapping("/coordinator/plan")
class CoordinatorController(
  private val planService: PlanService,
) {

  @PostMapping()
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Create a new sentence plan",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Plan created successfully"),
      ApiResponse(
        responseCode = "409",
        description = "Plan could not be created. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createPlan(@RequestBody createPlanRequest: CreatePlanRequest): PlanVersionResponse = planService.createPlan(createPlanRequest.planType)
    .run(PlanVersionResponse::from)

  @GetMapping("/{planUuid}")
  @Operation(
    description = "Gets the latest sentence plan, or a specific version if specified.",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan retrieved successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan or plan version not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Plan could not be loaded. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getPlan(
    @PathVariable planUuid: UUID,
  ): GetPlanResponse {
    try {
      return planService.getPlanVersionByPlanUuid(planUuid)
        .run(GetPlanResponse::from)
    } catch (_: EmptyResultDataAccessException) {
      throw NoResourceFoundException(HttpMethod.GET, "Could not find a plan with ID: $planUuid")
    }
  }

  @PostMapping("/{planUuid}/sign")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Signs the specified sentence plan, updating its status to 'AWAITING_COUNTERSIGN' and returning the latest version of the plan.",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan signed successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Plan could not be signed. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun signPlan(
    @PathVariable planUuid: UUID,
    @RequestBody signRequest: SignRequest,
  ): PlanVersionResponse {
    try {
      return planService.signPlan(planUuid, signRequest)
        .run(PlanVersionResponse::from)
    } catch (_: EmptyResultDataAccessException) {
      throw NoResourceFoundException(HttpMethod.GET, "Could not find a plan with ID: $planUuid")
    }
  }

  @PostMapping("/{planUuid}/lock")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Locks the specified sentence plan, updating its status to 'LOCKED_INCOMPLETE' and returning the latest version of the plan.",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan locked (incomplete) successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Plan could not be locked incomplete. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun lockPlan(
    @PathVariable planUuid: UUID,
    @RequestBody lockPlanRequest: LockPlanRequest,
  ): PlanVersionResponse = PlanVersionResponse.from(planService.lockPlan(planUuid))

  @PostMapping("/{planUuid}/countersign")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Updates the specified version of a sentence plan with the provided countersign status.",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan countersigned successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan or plan version not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Plan could not be countersigned. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun countersignPlan(
    @PathVariable planUuid: UUID,
    @RequestBody @Valid countersignPlanRequest: CounterSignPlanRequest,
  ): PlanVersionResponse = planService.countersignPlan(planUuid, countersignPlanRequest)
    .run(PlanVersionResponse::from)

  @PostMapping("/{planUuid}/rollback")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Sets the countersigning status of the specified plan version to 'ROLLED_BACK'",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan version rolled back successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan or plan version not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Plan version could not be rolled back. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun rollbackPlanVersion(
    @PathVariable planUuid: UUID,
    @RequestBody @Valid body: RollbackPlanRequest,
  ): PlanVersionResponse = PlanVersionResponse.from(planService.rollbackVersion(planUuid, body.sentencePlanVersion.toInt()))

  @PostMapping("/{planUuid}/soft-delete")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Sets the specified range of plan versions to soft deleted if all versions in specified range are not already set to soft deleted. If no upper range specified, the latest version is assumumed." +
      "",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan versions in the specified range have been set to soft deleted"),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found for the provided planUuid",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. The range is invalid or one or more versions in the specified range have already been set to soft deleted",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun softDeletePlanVersions(
    @PathVariable planUuid: UUID,
    @RequestBody @Valid body: SoftDeletePlanVersionsRequest,
  ) = planService.softDelete(
    planUuid,
    body.from.toInt(),
    body.to?.toInt(),
    true,
  )

  @PostMapping("/{planUuid}/restore")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Unsets the soft deleted flag for the specified range of plan versions to if all versions in specified range are already set to soft deleted. If no upper range specified, the latest version is assumumed." +
      "",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan versions in the specified range have had the set to soft deleted flag unset"),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found for the provided planUuid",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. The range is invalid or one or more versions in the specified range are not set to soft deleted",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun restorePlanVersions(
    @PathVariable planUuid: UUID,
    @RequestBody @Valid body: RestorePlanVersionsRequest,
  ) = planService.softDelete(
    planUuid,
    body.from.toInt(),
    body.to?.toInt(),
    false,
  )

  @PostMapping("/{planUuid}/clone")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_WRITE')")
  @Operation(
    description = "Clone the latest plan version data into a new version",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the latest plan version number and the plan UUID",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found for the provided planUuid",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun cloneLatestPlanVersion(
    @PathVariable planUuid: UUID,
    @RequestBody @Valid body: ClonePlanVersionRequest,
  ): PlanVersionResponse = PlanVersionResponse(planVersion = planService.clone(planUuid, body.planType).version.toLong(), planId = planUuid)
}
