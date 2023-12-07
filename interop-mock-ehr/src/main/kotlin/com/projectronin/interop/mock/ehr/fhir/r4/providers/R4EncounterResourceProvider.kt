package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4EncounterDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

@Component
class R4EncounterResourceProvider(override var resourceDAO: R4EncounterDAO) :
    BaseResourceProvider<Encounter, R4EncounterDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Encounter::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Encounter.SP_PATIENT) patient: ReferenceParam? = null,
        @OptionalParam(name = Encounter.SP_SUBJECT) subject: ReferenceParam? = null,
        @OptionalParam(name = Encounter.SP_DATE) dateRange: DateRangeParam? = null,
    ): List<Encounter> {
        val referenceList = mutableListOf<Reference>()

        patient?.let { referenceList.add(Reference("Patient/${it.value}")) }
        subject?.let { referenceList.add(Reference(subject.value)) }

        return resourceDAO.searchByQuery(
            referenceList,
            dateRange?.lowerBoundAsInstant,
            dateRange?.upperBoundAsInstant,
        )
    }
}
