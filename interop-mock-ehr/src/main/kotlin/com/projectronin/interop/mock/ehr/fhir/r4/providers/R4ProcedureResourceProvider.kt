package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ProcedureDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

@Component
class R4ProcedureResourceProvider(
    override var resourceDAO: R4ProcedureDAO,
) : BaseResourceProvider<Procedure, R4ProcedureDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Procedure::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Procedure.SP_PATIENT) patient: ReferenceParam? = null,
        @OptionalParam(name = Procedure.SP_SUBJECT) subject: ReferenceParam? = null,
        @OptionalParam(name = Procedure.SP_DATE) dateRange: DateRangeParam? = null,
    ): List<Procedure> {
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
