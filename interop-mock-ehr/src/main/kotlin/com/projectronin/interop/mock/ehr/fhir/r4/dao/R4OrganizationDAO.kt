package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Organization
import org.springframework.stereotype.Component

@Component
class R4OrganizationDAO(schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Organization>(context, schema, Organization::class.java) {
    /**
     * Finds Organizations based on input query parameters.
     * @param idList for filtering one or more Organization.id values. Treats id values in the list as a logical 'OR'.
     *               searchByIds with one id input is equivalent to an Organization.read.
     */
    fun searchByQuery(idList: StringOrListParam? = null): List<Organization> {
        return idList?.valuesAsQueryTokens?.mapNotNull { idParam ->
            try {
                idParam?.value?.let { id -> this.findById(id) }
            } catch (exception: ResourceNotFoundException) {
                null
            }
        } ?: emptyList()
    }
}
