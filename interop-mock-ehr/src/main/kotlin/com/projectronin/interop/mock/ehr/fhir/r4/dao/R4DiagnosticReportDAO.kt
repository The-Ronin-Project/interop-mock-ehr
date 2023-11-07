package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.DiagnosticReport
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4DiagnosticReportDAO(
    private val schema: SafeXDev,
    context: FhirContext
) : BaseResourceDAO<DiagnosticReport>(context, schema, DiagnosticReport::class.java) {
    /**
     * Finds the Diagnostic Report based on the input query parameters
     * @param reference the patientFhirId for the Diagnostic Report, i.e. 'Patient/eJcZl4T3ut30rE-k0LLiYyw3'
     * @param fromDate the earliest date to start searching for the Diagnostic Report
     * @param toDate the latest date to be searching the for Diagnostic Report
     */
    fun searchByQuery(
        patientId: String,
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<DiagnosticReport> {
        val parser = context.newJsonParser()

        // run query
        val diagnosticReports = schema.run(collection) {
            find("subject.reference = 'Patient/${patientId.escapeSQL()}'").execute().map {
                parser.parseResource(resourceType, it.toString())
            }
        }

        // compare and filter dates after query is run
        return diagnosticReports.filter { diagnosticReport ->
            if (diagnosticReport.hasEffectiveDateTimeType()) { // converts any null effective.value to "now"
                val effective = diagnosticReport.effectiveDateTimeType.value
                (toDate?.let { effective.before(it) } ?: true) && // upper bound
                    (fromDate?.let { effective.after(it) } ?: true) // lower bound
            } else {
                (fromDate == null) && (toDate == null)
            }
        }
    }
}
