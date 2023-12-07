package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4FlagDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Flag
import org.springframework.stereotype.Component

@Component
class R4FlagResourceProvider(override var resourceDAO: R4FlagDAO) :
    BaseResourceProvider<Flag, R4FlagDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Flag::class.java
    }

    @Search
    fun search(
        @RequiredParam(name = Flag.SP_PATIENT) patientReferenceParam: ReferenceParam,
    ): List<Flag> = resourceDAO.searchByQuery("Patient/${patientReferenceParam.value}")
}
