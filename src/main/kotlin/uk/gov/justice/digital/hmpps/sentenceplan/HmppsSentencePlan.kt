package uk.gov.justice.digital.hmpps.sentenceplan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class HmppsSentencePlan

fun main(args: Array<String>) {
  runApplication<HmppsSentencePlan>(*args)
}
