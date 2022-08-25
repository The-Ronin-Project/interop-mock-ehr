package com.projectronin.interop.mock.ehr.fhir.stu3.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.toDSTU3
import org.hl7.fhir.dstu3.model.Appointment
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import org.hl7.fhir.r4.model.Appointment as R4Appointment

@Component
class STU3AppointmentResourceProvider(override var resourceDAO: R4AppointmentDAO) :
    STU3BaseResourceProvider<Appointment, R4Appointment, R4AppointmentDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Appointment::class.java
    }

    @Search
    fun search(
        @RequiredParam(name = Appointment.SP_PATIENT) patientReferenceParam: ReferenceParam,
        @OptionalParam(name = Appointment.SP_DATE) dateRangeParam: DateRangeParam? = null,
        @OptionalParam(name = Appointment.SP_STATUS) statusParam: StringParam? = null,
        @OptionalParam(name = Appointment.SP_IDENTIFIER) identifierParam: TokenParam? = null
    ): List<Appointment> {

        identifierParam?.let {
            if (it.system == "mockEncounterCSNSystem") {
                return listOf(resourceDAO.findById(it.value).toDSTU3())
            } else throw UnsupportedOperationException("Identifier system '${it.system}' not supported.")
        }

        val referenceList = mutableListOf<Reference>()

        referenceList.add(Reference("Patient/${patientReferenceParam.value}"))

        return resourceDAO.searchByQuery(
            referenceList, dateRangeParam?.lowerBoundAsInstant, dateRangeParam?.upperBoundAsInstant, statusParam?.value
        ).map { it.toDSTU3() }
    }
}
