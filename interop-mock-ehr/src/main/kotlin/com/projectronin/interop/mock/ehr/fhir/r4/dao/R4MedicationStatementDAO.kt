package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.MedicationStatement
import org.springframework.stereotype.Component

@Component
class R4MedicationStatementDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<MedicationStatement>() {
    override var resourceType = MedicationStatement::class.java
    override var collection: Collection = database.createCollection(MedicationStatement::class.simpleName, true)

    /**
     * Finds medicationStatements based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering MedicationStatement.subject.reference values.
     */
    fun searchByQuery(
        subject: String? = null,
    ): List<MedicationStatement> {
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
