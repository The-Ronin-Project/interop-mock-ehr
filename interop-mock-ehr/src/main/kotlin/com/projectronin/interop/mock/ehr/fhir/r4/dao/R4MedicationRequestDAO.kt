package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationRequest
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4MedicationRequestDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationRequest>(context, schema, MedicationRequest::class.java) {
    /**
     * Finds medicationRequests based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering MedicationRequest.subject.reference values.
     * @param fromDate the earliest date to search based on medicationRequest.dosageInstructions[x].timing.repeat.boundsPeriod.start values.
     * @param toDate the latest date to search based on medicationRequest.dosageInstructions[x].timing.repeat.boundsPeriod.start values.
     */
    fun searchByQuery(
        subject: String? = null,
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<MedicationRequest> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }

        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        val medicationRequestList = mutableListOf<MedicationRequest>()
        val parser = context.newJsonParser()
        schema.run(collection) {
            find(query).execute().forEach {
                medicationRequestList.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        return medicationRequestList.filter { medReq ->
            // medicationRequests will be filtered out by EHRs when a fromDate or toDate is supplied
            // and there is NO date on the medicationRequest.  Since the date for the medicationRequest is contained in
            // the dosageInstruction, we will only keep medicationRequests which don't have a dosageInstruction
            // if neither toDate nor fromDate has been supplied.
            (medReq.dosageInstruction.isEmpty() && fromDate == null && toDate == null) ||
                medReq.dosageInstruction.any { dosage ->
                    (toDate?.let { dosage.timing?.repeat?.boundsPeriod?.start?.before(it) ?: false } ?: true) &&
                        (fromDate?.let { dosage.timing?.repeat?.boundsPeriod?.start?.after(it) ?: false } ?: true)
                }
        }
    }
}
