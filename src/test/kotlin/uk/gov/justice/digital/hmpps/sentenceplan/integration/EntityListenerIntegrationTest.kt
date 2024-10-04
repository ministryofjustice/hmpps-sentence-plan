package uk.gov.justice.digital.hmpps.sentenceplan.integration

import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.GoalRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanRepository
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanType
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionEntity
import uk.gov.justice.digital.hmpps.sentenceplan.entity.PlanVersionRepository
import uk.gov.justice.digital.hmpps.sentenceplan.services.PlanService

@DisplayName("Entity Listener Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(username = "Tests|EntityListenerIntegrationTest")
class EntityListenerIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var areaOfNeedRepository: AreaOfNeedRepository

  @Autowired
  lateinit var planRepository: PlanRepository

  @Autowired
  lateinit var planVersionRepository: PlanVersionRepository

  @Autowired
  lateinit var goalRepository: GoalRepository

  @Autowired
  lateinit var planService: PlanService

  @Test
  @WithMockUser(username = "Tests|EntityListenerIntegrationTest")
  fun `should call @PrePersist when saving PlanVersionEntity`() {
    val planEntity: PlanEntity = planService.createPlan(
      planType = PlanType.INITIAL,
    )

    val planVersion = spyk(
      PlanVersionEntity(
        plan = planEntity,
        planId = planEntity.id!!,
      ),
    )

    planVersionRepository.save(planVersion)

    verify { planVersion.prePersist() }
  }

  @Test
  @Sql(
    scripts = [
      "/db/test/oasys_assessment_pk_data.sql",
      "/db/test/goals_data.sql",
    ],
    executionPhase = BEFORE_TEST_METHOD,
  )
  @Sql(
    scripts = [
      "/db/test/goals_cleanup.sql",
      "/db/test/oasys_assessment_pk_cleanup.sql",
    ],
    executionPhase = AFTER_TEST_METHOD,
  )
  fun `should call @PrePersist when saving GoalEntity`() {
    val planEntity: PlanEntity = planRepository.findAll().first()

    val planVersion = spyk(planEntity.currentVersion!!)

    val goalEntity: GoalEntity = spyk(
      GoalEntity(
        title = "Goal title",
        areaOfNeed = areaOfNeedRepository.findByNameIgnoreCase("Accommodation"),
        planVersion = planVersion,
      ),
    )

    goalRepository.save(goalEntity)

    verify { goalEntity.prePersist() }
  }
}
