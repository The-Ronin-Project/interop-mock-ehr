package com.projectronin.interop.mock.ehr.fhir.r4.providers

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Binary
import org.springframework.stereotype.Component

@Component
class R4BinaryResourceProvider(override var resourceDAO: R4BinaryDAO) :
    BaseResourceProvider<Binary, R4BinaryDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Binary::class.java
    }
}
