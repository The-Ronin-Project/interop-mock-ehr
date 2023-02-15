package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationRequest
import org.springframework.stereotype.Component

@Component
class R4MedicationRequestDAO(schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationRequest>(context, schema, MedicationRequest::class.java) {
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
        return collection.run { find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) } }
    }
}
