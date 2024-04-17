package uk.gov.justice.digital.hmpps.sentenceplan.controlller

// import org.springframework.security.access.prepost.PreAuthorize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreasRepository

// @PreAuthorize("hasRole('ROLE_SENTENCE_PLAN_RW')")
@RestController
@RequestMapping("/hello")
class HelloController(val areasRepository: AreasRepository) {

  @GetMapping("/{name}", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
  fun getHello(@PathVariable name: String): String {
    val randomNumber = Math.random() * 10
    val mapper = jacksonObjectMapper()
//    val area = mapper.writeValueAsString(areasRepository.findById(1).get())
    if (randomNumber > 3) {
      return "{\"message\": \"Hello $name\"}"
    } else {
      throw RuntimeException("Replicating 5xx")
    }
  }
}
