package uk.gov.justice.digital.hmpps.sentenceplan.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.sentenceplan.entity.RefDataEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.ReferenceDataRepository
import java.util.Optional

@Service
class ReferenceDataService(
  private val referenceDataRepository: ReferenceDataRepository
) {

  fun getQuestionReferenceData(id: Int): Optional<RefDataEntity> {
   return referenceDataRepository.findById(id)
  }

}