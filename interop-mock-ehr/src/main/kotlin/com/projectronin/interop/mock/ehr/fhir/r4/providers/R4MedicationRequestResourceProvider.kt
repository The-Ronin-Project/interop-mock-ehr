package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationRequestDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.MedicationRequest
import org.springframework.stereotype.Component

@Component
class R4MedicationRequestResourceProvider(override var resourceDAO: R4MedicationRequestDAO) :
    BaseResourceProvider<MedicationRequest, R4MedicationRequestDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationRequest::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = MedicationRequest.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = MedicationRequest.SP_DATE) dateRangeParam: DateRangeParam? = null,
        @OptionalParam(name = "-timing-boundsPeriod") cernerDateRangeParam: DateRangeParam? = null,
    ): List<MedicationRequest> {
        if (dateRangeParam != null && cernerDateRangeParam != null) {
            throw UnsupportedOperationException(
                "The MedicationRequest endpoint does not allow both optional parameters " +
                    "\"${MedicationRequest.SP_DATE}\" and \"-timing-boundsPeriod\" to be specified.",
            )
        }

        val subject = patientReferenceParam?.let { "Patient/${it.value}" }
        val dateRange = dateRangeParam ?: cernerDateRangeParam

        return resourceDAO.searchByQuery(
            subject,
            dateRange?.lowerBoundAsInstant,
            dateRange?.upperBoundAsInstant,
        )
    }
}
