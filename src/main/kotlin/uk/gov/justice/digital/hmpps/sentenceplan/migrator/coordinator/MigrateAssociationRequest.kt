package uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator

import java.util.UUID

data class MigrateAssociationRequest(
    val mappings: List<VersionMapping>,
    val entityUuidFrom: UUID,
    val entityUuidTo: UUID,
    val entityTypeFrom: String = "PLAN",
    val entityTypeTo: String = "AAP_PLAN",
)
