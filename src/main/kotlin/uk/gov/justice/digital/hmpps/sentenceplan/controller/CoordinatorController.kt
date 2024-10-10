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
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.CounterSignPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.RollbackPlanRequest
import uk.gov.justice.digital.hmpps.sentenceplan.entity.request.SignRequest
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
  fun createPlan(@RequestBody createPlanRequest: CreatePlanRequest): PlanVersionResponse {
    return planService.createPlan(createPlanRequest.planType)
      .run(PlanVersionResponse::from)
  }

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

  @PostMapping("/{planUuid}/clone")
  @Operation(
    description = "Clones an existing sentence plan, creating a new plan with the same structure and details as the specified plan.",
    tags = ["Integrations"],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Plan cloned successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Plan not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "The plan could not be cloned. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun clonePlan(
    @PathVariable planUuid: UUID,
  ): PlanVersionResponse {
    /**
     * TODO: Implement logic to clone the existing sentence plan identified by 'planUuid'.
     *  - Retrieve the original plan using 'planUuid'.
     *  - Duplicate the plan's structure and details (goals, steps, ?notes?)
     *  - Set plan version back to 0, countersigning_status to UNSIGNED
     *  - Save cloned plan and return the new UUID and version.
     *  - Handle any exceptions or edge cases (i.e plan not found, cloning failures).
     */
    return PlanVersionResponse(
      planId = UUID.randomUUID(),
      planVersion = 0,
    )
  }

  @PostMapping("/{planUuid}/sign")
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
    return PlanVersionResponse(
      planId = planUuid,
      planVersion = 10,
    )
  }

  @PostMapping("/{planUuid}/lock")
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
    @RequestBody @Valid body: CounterSignPlanRequest,
  ): PlanVersionResponse {
    /**
     * TODO: Implement logic to countersign the specified sentence plan version
     *  - Retrieve the plan using 'planUuid' and it's specified 'sentencePlanVersion'
     *  - Check plan is in correct AWAITING_COUNTERSIGN/AWAITING_DOUBLE_COUNTERSIGN/LOCKED countersigning state
     *  - Update the plan countersigning_status with the 'SignType' from the request body
     *    - When doing this, make sure you DO NOT update the plan version number
     *  - Save the changes and return the the plan UUID and version number
     *  - Handle any exceptions or edge cases (i.e plan or version not found, invalid sign type, countersigning failures))
     */
    return PlanVersionResponse(
      planId = planUuid,
      planVersion = body.sentencePlanVersion,
    )
  }

  @PostMapping("/{planUuid}/rollback")
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

  /**
   * TODO: Implement logic for soft-deleting and restoring
   *  - A conversation regarding where this occurs needs to happen (could be inside the coordinator)
   */
//  fun softDeleteAllPlanVersions () {
//
//  }
//
//  fun restorePlanAllPlanVersions () {
//
//  }
}
