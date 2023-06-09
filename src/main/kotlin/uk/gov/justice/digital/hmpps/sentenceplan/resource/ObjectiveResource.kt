package uk.gov.justice.digital.hmpps.sentenceplan.resource

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.sentenceplan.model.CreateObjective
import uk.gov.justice.digital.hmpps.sentenceplan.model.Objective
import uk.gov.justice.digital.hmpps.sentenceplan.service.ObjectiveService
import java.util.UUID

@RestController
@RequestMapping("/sentence-plan/{sentencePlanId}/objective")
@PreAuthorize("hasRole('ROLE_SENTENCE_PLAN_RW')")
class ObjectiveResource(private val service: ObjectiveService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createObjective(@PathVariable sentencePlanId: UUID, @RequestBody objective: CreateObjective): Objective =
    service.createObjective(sentencePlanId, objective)

  @PutMapping("/{id}")
  fun updateObjective(
    @PathVariable sentencePlanId: UUID,
    @PathVariable id: UUID,
    @RequestBody objective: Objective,
  ): Objective =
    service.updateObjective(sentencePlanId, objective)

  @GetMapping
  fun listObjectives(@PathVariable sentencePlanId: UUID) = service.listObjectives(sentencePlanId)

  @GetMapping("/{id}")
  fun getSentencePlanObjective(@PathVariable sentencePlanId: UUID, @PathVariable id: UUID) = service.findObjective(id)

  @DeleteMapping("/{id}")
  fun deleteSentencePlanObjective(@PathVariable sentencePlanId: UUID, @PathVariable id: UUID) = service.deleteObjective(id)
}
