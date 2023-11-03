package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4ProcedureDAO(
    private val schema: SafeXDev,
    context: FhirContext
) : BaseResourceDAO<Procedure>(context, schema, Procedure::class.java) {
    /**
     * Finds Procedure based on input query parameters.
     * @param reference the patientFhirId for the PROCEDURES, i.e. 'Patient/eJcZl4T3ut30rE-k0LLiYyw3'
     * @param fromDate the earliest date to start searching for the Procedure
     * @param toDate the latest date to be searching the for Procedure
     */
    fun searchByQuery(
        reference: List<Reference> = listOf(),
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<Procedure> {
        val queryFragments = mutableListOf<String>()

        reference.forEach { ref ->
            ref.reference?.let { queryFragments.add("'${it.escapeSQL()}' = subject.reference") }
        }

        val query = queryFragments.joinToString(" AND ")
        val procedures = mutableListOf<Procedure>()
        val parser = context.newJsonParser()

        // run query
        schema.run(collection) {
            find(query).execute().forEach {
                procedures.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        return procedures.filter { procedure ->
            if (procedure.hasPerformedDateTimeType()) {
                val performed = procedure.performedDateTimeType.value
                (toDate?.let { performed.before(it) } ?: true) &&
                    (fromDate?.let { performed.after(it) } == true)
            } else {
                (fromDate == null) && (toDate == null)
            }
        }
    }
}
