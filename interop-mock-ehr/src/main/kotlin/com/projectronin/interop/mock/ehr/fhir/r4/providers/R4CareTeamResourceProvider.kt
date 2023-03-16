package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CareTeamDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CareTeam
import org.springframework.stereotype.Component

@Component
class R4CareTeamResourceProvider(override var resourceDAO: R4CareTeamDAO) :
    BaseResourceProvider<CareTeam, R4CareTeamDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return CareTeam::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = CareTeam.SP_PATIENT) patientReferenceParam: ReferenceParam? = null,
        @OptionalParam(name = CareTeam.SP_STATUS) statusParam: StringParam? = null
    ): List<CareTeam> {
        val patientReference = patientReferenceParam?.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(
            patientReference,
            statusParam?.value
        )
    }
}
