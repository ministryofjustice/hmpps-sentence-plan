package uk.gov.justice.digital.hmpps.sentenceplan.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.StepRepository
import java.util.UUID

@Service
class StepService(
  private val stepRepository: StepRepository,
) {

  fun getStepByUuid(stepUuid: UUID): StepEntity? = stepRepository.findByUuid(stepUuid)
}
