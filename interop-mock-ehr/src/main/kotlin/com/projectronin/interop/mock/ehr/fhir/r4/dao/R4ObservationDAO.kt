package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
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
     * @param category string for filtering Condition.category[*].coding[*].code or .category[*].text.
     */
    fun searchByQuery(
        subject: String? = null,
        category: String? = null,
    ): List<Observation> {

        // Build queryFragments into query conditions joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        category?.let { queryFragments.add("('$it' in category[*].coding[*].code OR '$it' in category[*].text)") }

        // Join query conditions with 'AND'
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of Condition resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().map { parser.parseResource(resourceType, it.toString()) }
    }
}
