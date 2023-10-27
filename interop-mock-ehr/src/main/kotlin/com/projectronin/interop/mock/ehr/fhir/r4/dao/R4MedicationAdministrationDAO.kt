package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationAdministration
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4MedicationAdministrationDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationAdministration>(context, schema, MedicationAdministration::class.java) {

    fun searchByPatient(
        patientID: String,
        fromDate: Date? = null,
        toDate: Date? = null
    ): List<MedicationAdministration> {
        val parser = context.newJsonParser()

        val medicationAdministrations = schema.run(collection) {
            find("subject.reference = 'Patient/${patientID.escapeSQL()}'").execute().map {
                parser.parseResource(resourceType, it.toString())
            }
        }

        // no good way to compare dates in the query string, so we have to filter post-query.
        return medicationAdministrations.filter { medicationAdministration ->
            if (medicationAdministration.hasEffectiveDateTimeType()) {
                val effective = medicationAdministration.effectiveDateTimeType.value
                (toDate?.let { effective.before(it) } ?: true) && // before upper bound?
                    (fromDate?.let { effective.after(it) } ?: true) // and after lower bound?
            } else {
                (fromDate == null) && (toDate == null)
            }
        }
    }

    fun searchByRequest(requestID: String): List<MedicationAdministration> {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        return schema.run(collection) {
            find("request.reference = 'MedicationRequest/${requestID.escapeSQL()}'")
                .execute().mapNotNull {
                    parser.parseResource(resourceType, it.toString())
                }
        }
    }
}
