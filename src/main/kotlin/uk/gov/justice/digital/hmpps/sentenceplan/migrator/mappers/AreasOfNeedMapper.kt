package uk.gov.justice.digital.hmpps.sentenceplan.migrator.mappers

import uk.gov.justice.digital.hmpps.sentenceplan.entity.AreaOfNeedEntity

class AreasOfNeedMapper {
  companion object {
    fun map(areaOfNeed: AreaOfNeedEntity) = when (areaOfNeed.name) {
      "Accommodation" -> "accommodation"
      "Employment and education" -> "employment-and-education"
      "Drug use" -> "drug-use"
      "Alcohol use" -> "alcohol-use"
      "Health and wellbeing" -> "health-and-wellbeing"
      "Personal relationships and community" -> "personal-relationships-and-community"
      "Thinking, behaviours and attitudes" -> "thinking-behaviours-and-attitudes"
      "Finances" -> "finances"
      else -> ""
    }
  }
}
