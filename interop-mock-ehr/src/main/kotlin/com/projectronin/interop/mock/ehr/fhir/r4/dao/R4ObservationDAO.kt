package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Observation
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4ObservationDAO(schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Observation>(context, schema, Observation::class.java) {
    /**
     * Finds Observations based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering Observation.subject.reference values.
     * @param category is for filtering multiple coded values.
     *         Supports FHIR token syntax for inputting system|code.
     *         For system|code and system| it matches on Observation.category[*].coding[*].code or .system as indicated.
     *         For |code and code it matches on Observation.category[*].coding[*].code or .category[*].text values.
     */
    fun searchByQuery(
        subject: String? = null,
        category: TokenOrListParam? = null,
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<Observation> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }
        category?.let { catList ->
            val phrase = getSearchStringForFHIRTokens(catList)
            if (!phrase.isNullOrEmpty()) {
                queryFragments.add(phrase)
            }
        }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val observationList = mutableListOf<Observation>()
        val parser = context.newJsonParser()
        collection.run {
            find(query).execute().forEach {
                observationList.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        // no good way to compare dates in the query string, so we have to filter post-query.
        return observationList.filter { observation ->
            // support the RCDM Observation.effective types - accept the entry unless a date filter exists and fails
            when {
                observation.hasEffectiveDateTimeType() -> {
                    val date = observation.getEffectiveDateTimeType() // hapi converts any null effective.value to "now"
                    (toDate?.let { date.value.before(it) || date.value.equals(it) } ?: true) &&
                        (fromDate?.let { date.value.after(it) || date.value.equals(it) } ?: true)
                }
                observation.hasEffectivePeriod() -> {
                    val period = observation.getEffectivePeriod() // hapi returns the value or an empty Period()
                    (toDate?.let { period.end?.let { end -> end.before(it) || (end == it) } } ?: true) &&
                        (fromDate?.let { period.start?.let { start -> start.after(it) || (start == it) } } ?: true)
                }
                else -> true
            }
        }
    }
}
