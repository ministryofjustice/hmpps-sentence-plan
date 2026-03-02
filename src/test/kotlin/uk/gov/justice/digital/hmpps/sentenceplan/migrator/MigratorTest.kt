package uk.gov.justice.digital.hmpps.sentenceplan.migrator

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.springframework.data.domain.Page
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import kotlin.test.Test

class MigratorTest {
  val planRepository = mockk<PlanRepository>()
  val planVersionRepository = mockk<PlanVersionRepository>()
  val assessmentPlatformClient = mockk<WebClient>()
  val coordinatorClient = mockk<WebClient>()
  val migrator = spyk(
    Migrator(planRepository, planVersionRepository, assessmentPlatformClient, coordinatorClient),
  )

  val planFor = { id: Long ->
    PlanEntity(
      id = id,
      migrated = false,
    )
  }

  val planVersionFor = { plan: PlanEntity ->
    PlanVersionEntity(
      id = 1L,
      plan = plan,
      planId = plan.id!!,
    )
  }

  val plans = (1..2).map { planFor(it.toLong()) }

  @Nested
  inner class Run {
    val page1 = mockk<Page<PlanEntity>>()
    val page2 = mockk<Page<PlanEntity>>()

    @BeforeEach
    fun beforeEach() {
      every { page1.content } returns listOf(plans[0])
      every { page1.hasContent() } returns page1.content.isNotEmpty()
      every { page1.totalPages } returns 1
      every { migrator.migrate(any()) } just Runs
    }

    @Test
    fun `it runs`() {
      every { page1.hasNext() } returns false

      every { planRepository.findAllByMigratedFalse(any()) } returns page1

      migrator.run()

      verify(exactly = 1) { planRepository.findAllByMigratedFalse(any()) }
    }

    @Test
    fun `it grabs another page if there is one available`() {
      every { page1.hasNext() } returns true
      every { page2.hasNext() } returns false
      every { page2.content } returns listOf(plans[1])
      every { page2.hasContent() } returns page2.content.isNotEmpty()

      every { planRepository.findAllByMigratedFalse(match { it.pageNumber == 0 }) } returns page1
      every { planRepository.findAllByMigratedFalse(match { it.pageNumber == 1 }) } returns page2

      migrator.run()

      verify(exactly = 2) { planRepository.findAllByMigratedFalse(any()) }
    }
  }
}
