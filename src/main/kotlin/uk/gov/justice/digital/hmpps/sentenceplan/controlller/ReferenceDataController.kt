package uk.gov.justice.digital.hmpps.sentenceplan.controlller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.service.ReferenceDataService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.sentenceplan.entity.RefData

@RestController
@RequestMapping("/question-reference-data")
class ReferenceDataController(private val service: ReferenceDataService) {

  @GetMapping(produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
  fun getReferenceData(): String {
    val mapper = jacksonObjectMapper()
    val json = service.getQuestionReferenceData(1).get().refData
    val refData = mapper.readValue(json, RefData::class.java)
    return json
  }

  @GetMapping("/{id}", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
  fun getReferenceDataById(@PathVariable id: Int): String {
    return service.getQuestionReferenceData(id).get().refData
  }
}