package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DocumentReference
import org.springframework.stereotype.Component

@Component
class R4DocumentReferenceResourceProvider(override var resourceDAO: R4DocumentReferenceDAO) :
    BaseResourceProvider<DocumentReference, R4DocumentReferenceDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return DocumentReference::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Condition.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Condition.SP_SUBJECT) subjectReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Condition.SP_ENCOUNTER) encounterReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = Condition.SP_CATEGORY) categoryParam: TokenOrListParam? = null,
        @OptionalParam(name = "docStatus") docStatusParam: StringParam? = null,
    ): List<DocumentReference> {
        val subject = patientReferenceParam?.let { "Patient/${it.value}" } ?: subjectReferenceParam?.value
        val encounter = encounterReferenceParam?.let { "Encounter/${it.value}" }
        return resourceDAO.searchByQuery(
            subject,
            categoryParam,
            docStatusParam?.value,
            encounter
        )
    }
}
