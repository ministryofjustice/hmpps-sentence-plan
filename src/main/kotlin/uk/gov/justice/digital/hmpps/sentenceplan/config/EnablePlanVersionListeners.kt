package uk.gov.justice.digital.hmpps.sentenceplan.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.services.VersionService

@Configuration
class EnablePlanVersionListeners(private val versionService: VersionService) {
  @PostConstruct
  fun init() {
    GoalEntity.setVersionService(versionService)
  }
}
