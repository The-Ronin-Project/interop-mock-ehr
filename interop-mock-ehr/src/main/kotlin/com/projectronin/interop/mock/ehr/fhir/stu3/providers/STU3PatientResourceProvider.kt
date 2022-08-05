package com.projectronin.interop.mock.ehr.fhir.stu3.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.stu3.dao.STU3PatientDAO
import org.hl7.fhir.dstu3.model.ContactPoint
import org.hl7.fhir.dstu3.model.Identifier
import org.hl7.fhir.dstu3.model.Patient
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.stereotype.Component

@Component
class STU3PatientResourceProvider(override var resourceDAO: STU3PatientDAO) :
    STU3BaseResourceProvider<Patient, STU3PatientDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Patient::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Patient.SP_BIRTHDATE) bd: DateParam? = null,
        @OptionalParam(name = Patient.SP_GIVEN) givenName: StringParam? = null,
        @OptionalParam(name = Patient.SP_FAMILY) familyName: StringParam? = null,
        @OptionalParam(name = Patient.SP_GENDER) gender: StringParam? = null,
        @OptionalParam(name = Patient.SP_EMAIL) email: StringParam? = null,
        @OptionalParam(name = Patient.SP_TELECOM) telecomParam: TokenParam? = null,
    ): List<Patient> {

        return resourceDAO.searchByQuery(
            bd?.valueAsString,
            givenName?.value,
            familyName?.value,
            gender?.value,
            email?.value,
            telecomParam?.let { ContactPoint().setValue(it.value) }
        )
    }

    @Search
    fun searchByIdentifier(@RequiredParam(name = Patient.SP_IDENTIFIER) idToken: TokenParam): Patient? {
        val identifier = Identifier()
        identifier.value = idToken.value
        identifier.system = idToken.system
        return resourceDAO.searchByIdentifier(identifier)
    }
}
