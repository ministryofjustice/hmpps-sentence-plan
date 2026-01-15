package uk.gov.justice.digital.hmpps.sentenceplan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class HmppsSentencePlan

fun main(args: Array<String>) {
  runApplication<HmppsSentencePlan>(*args)
}
