package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.RequestGroup
import org.springframework.stereotype.Component

@Component
class R4RequestGroupDAO(
    schema: SafeXDev,
    context: FhirContext
) : BaseResourceDAO<RequestGroup>(context, schema, RequestGroup::class.java) {

    fun searchByQuery(
        idList: StringOrListParam? = null
    ): List<RequestGroup> {
        return idList?.valuesAsQueryTokens?.mapNotNull { ids ->
            try {
                ids?.value?.let { id -> this.findById(id) }
            } catch (exception: ResourceNotFoundException) {
                null
            }
        } ?: emptyList()
    }
}
