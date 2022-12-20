package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Observation
import org.springframework.stereotype.Component

@Component
class R4ObservationDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Observation>() {
    override var resourceType = Observation::class.java
    override var collection: Collection = database.createCollection(Observation::class.simpleName, true)

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
    ): List<Observation> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        category?.let { catList ->
            val phrase = getSearchStringForFHIRTokens(catList)
            if (!phrase.isNullOrEmpty()) {
                queryFragments.add(phrase)
            }
        }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
    }
}
