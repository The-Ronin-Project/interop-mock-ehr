package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationRequestDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.MedicationRequest
import org.springframework.stereotype.Component

@Component
class R4MedicationRequestResourceProvider(override var resourceDAO: R4MedicationRequestDAO) :
    BaseResourceProvider<MedicationRequest, R4MedicationRequestDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationRequest::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = MedicationRequest.SP_PATIENT) patientReferenceParam: ReferenceParam? = null
    ): List<MedicationRequest> {
        val subject = patientReferenceParam?.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(subject)
    }
}
