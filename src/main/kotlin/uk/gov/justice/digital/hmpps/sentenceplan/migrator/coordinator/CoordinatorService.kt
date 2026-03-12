package uk.gov.justice.digital.hmpps.sentenceplan.migrator.coordinator

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class CoordinatorService(
  @param:Qualifier("coordinatorClient")
  private val coordinatorClient: WebClient,
) {
  fun migrateAssociations(request: MigrateAssociationRequest) {
    coordinatorClient
      .post()
      .uri("/oasys/migrate-associations")
      .bodyValue(request)
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
