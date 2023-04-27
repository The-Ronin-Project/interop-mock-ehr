package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationStatement
import org.springframework.stereotype.Component

@Component
class R4MedicationStatementDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationStatement>(context, schema, MedicationStatement::class.java) {
    /**
     * Finds medicationStatements based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering MedicationStatement.subject.reference values.
     */
    fun searchByQuery(
        subject: String? = null
    ): List<MedicationStatement> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }

        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return schema.run(collection) {
            find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
        }
    }
}
