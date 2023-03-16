package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

@Component
class R4AppointmentResourceProvider(override var resourceDAO: R4AppointmentDAO) :
    BaseResourceProvider<Appointment, R4AppointmentDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Appointment::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Appointment.SP_ACTOR) referenceParam: ReferenceParam? = null,
        @OptionalParam(name = Appointment.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Appointment.SP_PRACTITIONER) practitionerReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Appointment.SP_DATE) dateRangeParam: DateRangeParam? = null,
        @OptionalParam(name = Appointment.SP_LOCATION) locationParam: ReferenceParam? = null
    ): List<Appointment> {
        val referenceList = mutableListOf<Reference>()

        referenceParam?.resourceType?.let { referenceList.add(Reference(referenceParam.value)) }
        patientReferenceParam?.let { referenceList.add(Reference("Patient/${it.value}")) }
        practitionerReferenceParam?.let { referenceList.add(Reference("Practitioner/${it.value}")) }
        locationParam?.let { referenceList.add(Reference("Location/${it.value}")) }

        return resourceDAO.searchByQuery(
            referenceList,
            dateRangeParam?.lowerBoundAsInstant,
            dateRangeParam?.upperBoundAsInstant
        )
    }
}
