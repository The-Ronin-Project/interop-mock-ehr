package com.projectronin.interop.mock.ehr.fhir.stu3.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import com.projectronin.interop.mock.ehr.fhir.stu3.dao.STU3AppointmentDAO
import org.hl7.fhir.dstu3.model.Appointment
import org.hl7.fhir.dstu3.model.Reference
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.stereotype.Component

@Component
class STU3AppointmentResourceProvider(override var resourceDAO: STU3AppointmentDAO) :
    STU3BaseResourceProvider<Appointment, STU3AppointmentDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Appointment::class.java
    }

    @Search
    fun search(
        @RequiredParam(name = Appointment.SP_PATIENT) patientReferenceParam: ReferenceParam,
        @OptionalParam(name = Appointment.SP_DATE) dateRangeParam: DateRangeParam? = null,
        @OptionalParam(name = Appointment.SP_STATUS) statusParam: StringParam? = null,
    ): List<Appointment> {
        val referenceList = mutableListOf<Reference>()

        referenceList.add(Reference("Patient/${patientReferenceParam.value}"))

        return resourceDAO.searchByQuery(
            referenceList, dateRangeParam?.lowerBoundAsInstant, dateRangeParam?.upperBoundAsInstant, statusParam?.value
        )
    }
}
