package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.Observation
import org.springframework.stereotype.Component

@Component
class R4ObservationDAO(database: Schema) : BaseResourceDAO<Observation>() {
    override var context: FhirContext = FhirContext.forR4()
    override var resourceType = Observation::class.java
    override var collection: Collection = database.createCollection(Observation::class.simpleName, true)

    /**
     * Finds conditions based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering Condition.subject.reference values.
     * @param category is for filtering multiple coded values.
     *         Supports FHIR token syntax for inputting system|code.
     *         For system|code and system| it matches on Condition.category[*].coding[*].code or .system as indicated.
     *         For |code and code it matches on Condition.category[*].coding[*].code or .category[*].text values.
     */
    fun searchByQuery(
        subject: String? = null,
        category: TokenOrListParam? = null,
    ): List<Observation> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        category?.let { fhirtokens ->
            getSearchStringForFHIRTokens(fhirtokens)?.let { searchString ->
                queryFragments.add(searchString)
            }
        }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
    }
}
