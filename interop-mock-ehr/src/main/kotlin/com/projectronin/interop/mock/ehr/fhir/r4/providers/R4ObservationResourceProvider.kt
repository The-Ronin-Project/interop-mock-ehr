package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.BaseResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ObservationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Observation
import org.springframework.stereotype.Component

@Component
class R4ObservationResourceProvider(override var resourceDAO: R4ObservationDAO) :
    BaseResourceProvider<Observation, R4ObservationDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Observation::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Observation.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Observation.SP_CATEGORY) categoryParam: TokenParam? = null,
    ): List<Observation> {
        val reference = patientReferenceParam?.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(
            reference,
            categoryParam?.value
        )
    }
}
