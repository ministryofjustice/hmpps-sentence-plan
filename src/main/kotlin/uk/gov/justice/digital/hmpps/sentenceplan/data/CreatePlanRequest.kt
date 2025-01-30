package uk.gov.justice.digital.hmpps.sentenceplan.data

import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType

class CreatePlanRequest(
  val planType: PlanType,
  override val userDetails: UserDetails,
) : CoordinatorRequest(userDetails)
