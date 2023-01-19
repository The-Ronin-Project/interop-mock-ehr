package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.MedicationRequest
import org.springframework.stereotype.Component

@Component
class R4MedicationRequestDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<MedicationRequest>() {
    override var resourceType = MedicationRequest::class.java
    override var collection: Collection = database.createCollection(MedicationRequest::class.simpleName, true)

    /**
     * Finds medicationRequests based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering MedicationRequest.subject.reference values.
     */
    fun searchByQuery(
        subject: String? = null,
    ): List<MedicationRequest> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }

        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
    }
}
