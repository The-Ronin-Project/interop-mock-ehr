package com.projectronin.interop.mock.ehr.fhir.r4.providers

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CommunicationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Communication
import org.springframework.stereotype.Component

@Component
class R4CommunicationResourceProvider(override var resourceDAO: R4CommunicationDAO) :
    BaseResourceProvider<Communication, R4CommunicationDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Communication::class.java
    }
}
