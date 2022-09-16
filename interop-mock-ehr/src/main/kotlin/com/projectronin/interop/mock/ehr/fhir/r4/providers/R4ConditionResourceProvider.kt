package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ConditionDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Condition
import org.springframework.stereotype.Component

@Component
class R4ConditionResourceProvider(override var resourceDAO: R4ConditionDAO) :
    BaseResourceProvider<Condition, R4ConditionDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return Condition::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Condition.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Condition.SP_CATEGORY) categoryParam: TokenOrListParam? = null,
        @OptionalParam(name = Condition.SP_CLINICAL_STATUS) clinicalStatusParam: TokenParam? = null,
    ): List<Condition> {
        val reference = patientReferenceParam?.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(
            reference,
            categoryParam,
            clinicalStatusParam?.value,
        )
    }
}
