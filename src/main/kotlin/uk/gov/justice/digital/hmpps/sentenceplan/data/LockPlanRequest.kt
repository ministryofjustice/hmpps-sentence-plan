package uk.gov.justice.digital.hmpps.sentenceplan.data

data class LockPlanRequest(override val userDetails: UserDetails) : CoordinatorRequest(userDetails)
