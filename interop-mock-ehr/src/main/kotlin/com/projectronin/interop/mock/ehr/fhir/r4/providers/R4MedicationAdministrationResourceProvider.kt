package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationAdministrationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.MedicationAdministration
import org.springframework.stereotype.Component

@Component
class R4MedicationAdministrationResourceProvider(override var resourceDAO: R4MedicationAdministrationDAO) :
    BaseResourceProvider<MedicationAdministration, R4MedicationAdministrationDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationAdministration::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = MedicationAdministration.SP_REQUEST) requestParam: ReferenceParam? = null,
        @OptionalParam(name = MedicationAdministration.SP_PATIENT) patientParam: ReferenceParam? = null,
        @OptionalParam(name = MedicationAdministration.SP_EFFECTIVE_TIME) effectiveTimeParam: DateRangeParam? = null
    ): List<MedicationAdministration> {
        if (requestParam != null) {
            return resourceDAO.searchByRequest(requestParam.value)
        }

        if (patientParam != null) {
            return resourceDAO.searchByPatient(
                patientParam.value,
                effectiveTimeParam?.lowerBoundAsInstant,
                effectiveTimeParam?.upperBoundAsInstant
            )
        }

        throw IllegalArgumentException("Either request or patient must be provided")
    }
}
