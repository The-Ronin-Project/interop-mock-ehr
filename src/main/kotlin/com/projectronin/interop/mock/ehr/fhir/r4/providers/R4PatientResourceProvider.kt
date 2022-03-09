package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.BaseResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component

@Component
class R4PatientResourceProvider(override var resourceDAO: R4PatientDAO) :
    BaseResourceProvider<Patient, R4PatientDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Patient::class.java
    }

    @Search
    fun searchByBirth(@RequiredParam(name = Patient.SP_BIRTHDATE) bd: DateParam): List<Patient> {
        return resourceDAO.searchByBirthdate(bd.valueAsString)
    }

    @Search
    fun searchByIdentifier(@RequiredParam(name = Patient.SP_IDENTIFIER) idToken: TokenParam): Patient {
        val identifier = Identifier()
        identifier.value = idToken.value
        identifier.system = idToken.system
        return resourceDAO.searchByIdentifier(identifier)
    }
}
