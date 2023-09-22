package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationStatement
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4MedicationStatementDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationStatement>(context, schema, MedicationStatement::class.java) {
    /**
     * Finds medicationStatements based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering MedicationStatement.subject.reference values.
     * @param fromDate the earliest date to search based on medicationStatement.effectivePeriod.start.
     * @param toDate the latest date to search based on medicationStatement.effectivePeriod.start.
     */
    fun searchByQuery(
        subject: String? = null,
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<MedicationStatement> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }

        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        val medicationStatementList = mutableListOf<MedicationStatement>()
        val parser = context.newJsonParser()
        schema.run(collection) {
            find(query).execute().forEach {
                medicationStatementList.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        return medicationStatementList.filter { medStatement ->
            (toDate?.let { medStatement.effectivePeriod.start.before(it) } ?: true) &&
                (fromDate?.let { medStatement.effectivePeriod.start.after(it) } ?: true)
        }
    }
}
