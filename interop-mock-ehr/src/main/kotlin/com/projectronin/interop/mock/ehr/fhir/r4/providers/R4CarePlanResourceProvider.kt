package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CarePlanDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CarePlan
import org.springframework.stereotype.Component

@Component
class R4CarePlanResourceProvider(override var resourceDAO: R4CarePlanDAO) :
    BaseResourceProvider<CarePlan, R4CarePlanDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return CarePlan::class.java
    }

    @Search
    fun search(
        @RequiredParam(name = CarePlan.SP_PATIENT) patientReferenceParam: ReferenceParam,
        @RequiredParam(name = CarePlan.SP_CATEGORY) categoryParam: TokenParam,
        @OptionalParam(name = CarePlan.SP_DATE) dateRangeParam: DateRangeParam? = null
    ): List<CarePlan> {
        val reference = patientReferenceParam.let { "Patient/${it.value}" }
        val category = categoryParam.value
        return resourceDAO.searchByQuery(
            reference,
            category,
            dateRangeParam?.lowerBoundAsInstant,
            dateRangeParam?.upperBoundAsInstant
        )
    }
}
