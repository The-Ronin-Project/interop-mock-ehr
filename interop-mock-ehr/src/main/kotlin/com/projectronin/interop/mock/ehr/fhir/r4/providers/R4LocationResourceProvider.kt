package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.springframework.stereotype.Component

@Component
class R4LocationResourceProvider(override var resourceDAO: R4LocationDAO) :
    BaseResourceProvider<Location, R4LocationDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Location::class.java
    }

    @Search
    fun searchByIdentifier(
        @RequiredParam(name = Location.SP_IDENTIFIER) idToken: TokenParam,
    ): Location? {
        val identifier = Identifier()
        identifier.value = idToken.value
        identifier.system = idToken.system
        return resourceDAO.searchByIdentifier(identifier)
    }
}
