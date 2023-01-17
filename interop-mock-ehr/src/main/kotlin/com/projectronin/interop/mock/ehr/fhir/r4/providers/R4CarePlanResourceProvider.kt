package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CarePlanDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.springframework.stereotype.Component

@Component
class R4CarePlanResourceProvider(override var resourceDAO: R4CarePlanDAO) :
    BaseResourceProvider<CarePlan, R4CarePlanDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return CarePlan::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Condition.SP_PATIENT) patientReferenceParam: ReferenceParam? = null
    ): List<CarePlan> {
        val reference = patientReferenceParam?.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(
            reference
        )
    }
}
