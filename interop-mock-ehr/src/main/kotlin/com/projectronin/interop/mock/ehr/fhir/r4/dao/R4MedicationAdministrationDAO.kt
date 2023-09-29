package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.MedicationAdministration
import org.springframework.stereotype.Component

@Component
class R4MedicationAdministrationDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<MedicationAdministration>(context, schema, MedicationAdministration::class.java) {

    fun searchByRequest(requestID: String): List<MedicationAdministration> {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        return schema.run(collection) {
            find("'MedicationRequest/${requestID.escapeSQL()}' in request.reference")
                .execute().mapNotNull {
                    parser.parseResource(resourceType, it.toString())
                }
        }
    }
}
