package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ServiceRequestDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.ServiceRequest
import org.springframework.stereotype.Component

@Component
class R4ServiceRequestResourceProvider(override var resourceDAO: R4ServiceRequestDAO) :
    BaseResourceProvider<ServiceRequest, R4ServiceRequestDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return ServiceRequest::class.java
    }

    @Search
    fun search(@RequiredParam(name = ServiceRequest.SP_PATIENT) patientReferenceParam: ReferenceParam): List<ServiceRequest> {
        return resourceDAO.searchByQuery("Patient/${patientReferenceParam.value}")
    }
}
