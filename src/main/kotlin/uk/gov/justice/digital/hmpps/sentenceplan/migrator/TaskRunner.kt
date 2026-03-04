package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import org.springframework.beans.factory.getBean
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import uk.gov.justice.digital.hmpps.sentenceplan.HmppsSentencePlan

object TaskRunner {

  @JvmStatic
  fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext =
      SpringApplicationBuilder(HmppsSentencePlan::class.java)
        .run(*args)

    val migrator = context.getBean<Migrator>()
    migrator.run()

    context.close()
  }
}
