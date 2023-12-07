package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.Transaction
import ca.uhn.fhir.rest.annotation.TransactionParam
import ca.uhn.fhir.rest.server.IResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.dao.BaseResourceDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.ResourceType
import org.springframework.stereotype.Component

@Component
class R4BundleResourceProvider(resourceDAOs: List<BaseResourceDAO<*>>) : IResourceProvider {
    private val daoMap: Map<ResourceType, BaseResourceDAO<*>> =
        // Spring auto-wires all Beans that implement BaseResourceDAO
        resourceDAOs.associateBy { ResourceType.fromCode(it.resourceType.simpleName) }

    override fun getResourceType(): Class<out IBaseResource> {
        return Bundle::class.java
    }

    @Transaction
    fun bundleTransaction(
        @TransactionParam bundleParam: Bundle,
    ): Bundle {
        val response = Bundle()
        bundleParam.entry.forEach { entry ->
            if (entry.request.method != Bundle.HTTPVerb.POST) {
                throw UnsupportedOperationException("This server only allows POST operations in transaction Bundles.")
            }
            val resourceDAO = daoMap[entry.resource.resourceType]
            val id =
                resourceDAO?.insert(entry.resource)
                    ?: throw UnsupportedOperationException("Resource type ${entry.resource.resourceType} not supported.")
            val responseEntry = BundleEntryComponent()
            responseEntry.resource = entry.resource // FHIR example shows full resource returned.
            responseEntry.response =
                Bundle.BundleEntryResponseComponent().setStatus("201 Created")
                    // HAPI serialization prepends IDs with the resource type but BaseResourceDAO does not.
                    .setLocation("${entry.resource.resourceType}/${id.removePrefix("${entry.resource.resourceType}/")}")
            response.addEntry(responseEntry)
        }
        return response
    }
}
