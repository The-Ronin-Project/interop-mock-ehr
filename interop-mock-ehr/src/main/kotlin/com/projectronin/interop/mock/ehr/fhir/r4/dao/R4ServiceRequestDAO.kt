package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.ServiceRequest
import org.springframework.stereotype.Component

@Component
class R4ServiceRequestDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<ServiceRequest>(context, schema, ServiceRequest::class.java) {
    /**
     * Finds ServiceRequests by [subject]
     */
    fun searchByQuery(subject: String): List<ServiceRequest> {
        val parser = context.newJsonParser()
        return schema.run(collection) {
            find("'${subject.escapeSQL()}' = subject.reference").execute()
                .mapNotNull { parser.parseResource(resourceType, it.toString()) }
        }
    }
}
