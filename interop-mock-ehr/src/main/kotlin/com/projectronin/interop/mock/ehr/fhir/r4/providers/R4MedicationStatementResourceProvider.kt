package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationStatementDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.MedicationStatement
import org.springframework.stereotype.Component

@Component
class R4MedicationStatementResourceProvider(override var resourceDAO: R4MedicationStatementDAO) :
    BaseResourceProvider<MedicationStatement, R4MedicationStatementDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationStatement::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = MedicationStatement.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = MedicationStatement.SP_EFFECTIVE) dateRangeParam: DateRangeParam? = null,
    ): List<MedicationStatement> {
        val subject = patientReferenceParam?.let { "Patient/${it.value}" }

        return resourceDAO.searchByQuery(
            subject,
            dateRangeParam?.lowerBoundAsInstant,
            dateRangeParam?.upperBoundAsInstant,
        )
    }
}
