package uk.gov.justice.digital.hmpps.sentenceplan.controlller

// import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// @PreAuthorize("hasRole('ROLE_SENTENCE_PLAN_RW')")
@RestController
@RequestMapping("/hello")
class HelloController() {

  @GetMapping("/{name}", produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
  fun getHello(@PathVariable name: String) = run {
    val randomNumber = Math.random() * 10
    if (randomNumber > 3) {
      "{\"test\": \"Hello $name\"}"
    } else {
      throw RuntimeException("Replicating 5xx")
    }
  }
}
