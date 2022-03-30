package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.BaseResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner
import org.springframework.stereotype.Component

@Component
class R4PractitionerResourceProvider(override var resourceDAO: R4PractitionerDAO) :
    BaseResourceProvider<Practitioner, R4PractitionerDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Practitioner::class.java
    }

    @Search
    fun searchByIdentifier(@RequiredParam(name = Practitioner.SP_IDENTIFIER) idToken: TokenParam): Practitioner? {
        val identifier = Identifier()
        identifier.value = idToken.value
        identifier.system = idToken.system
        return resourceDAO.searchByIdentifier(identifier)
    }
}
