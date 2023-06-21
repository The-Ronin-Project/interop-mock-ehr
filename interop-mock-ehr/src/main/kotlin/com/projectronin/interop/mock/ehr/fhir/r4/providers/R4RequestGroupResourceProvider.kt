package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.StringOrListParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4RequestGroupDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.RequestGroup
import org.springframework.stereotype.Component

@Component
class R4RequestGroupResourceProvider(
    override var resourceDAO: R4RequestGroupDAO
) : BaseResourceProvider<RequestGroup, R4RequestGroupDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return RequestGroup::class.java
    }

    @Search
    fun searchByQuery(
        @RequiredParam(name = RequestGroup.SP_RES_ID) idListParam: StringOrListParam? = null
    ): List<RequestGroup> {
        return resourceDAO.searchByQuery(idListParam)
    }
}
