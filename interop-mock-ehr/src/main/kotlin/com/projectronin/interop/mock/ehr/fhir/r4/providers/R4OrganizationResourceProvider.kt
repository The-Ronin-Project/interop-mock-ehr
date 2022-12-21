package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.StringOrListParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4OrganizationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Organization
import org.springframework.stereotype.Component

@Component
class R4OrganizationResourceProvider(override var resourceDAO: R4OrganizationDAO) :
    BaseResourceProvider<Organization, R4OrganizationDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Organization::class.java
    }

    @Search
    fun searchByQuery(
        @OptionalParam(name = Organization.SP_RES_ID) idListParam: StringOrListParam? = null,
    ): List<Organization> {
        return resourceDAO.searchByQuery(
            idListParam
        )
    }
}
