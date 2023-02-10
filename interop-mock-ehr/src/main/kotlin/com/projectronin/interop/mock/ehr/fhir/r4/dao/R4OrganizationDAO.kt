package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Organization
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class R4OrganizationDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Organization>() {
    override var resourceType = Organization::class.java
    override var collection = AtomicReference(database.createCollection(Organization::class.simpleName, true))

    /**
     * Finds Organizations based on input query parameters.
     * @param idList for filtering one or more Organization.id values. Treats id values in the list as a logical 'OR'.
     *               searchByIds with one id input is equivalent to an Organization.read.
     */
    fun searchByQuery(
        idList: StringOrListParam? = null
    ): List<Organization> {
        return idList?.valuesAsQueryTokens?.mapNotNull { idParam ->
            try {
                idParam?.value?.let { id -> this.findById(id) }
            } catch (exception: ResourceNotFoundException) {
                null
            }
        } ?: emptyList()
    }
}
