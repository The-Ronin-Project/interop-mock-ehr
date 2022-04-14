package com.projectronin.interop.mock.ehr.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.DbDoc
import com.mysql.cj.xdevapi.JsonString
import org.hl7.fhir.r4.model.Resource
import java.util.UUID

abstract class BaseResourceDAO<T : Resource> {

    abstract var context: FhirContext
    abstract var resourceType: Class<T>
    abstract var collection: Collection

    fun insert(resource: T): String {
        if (!resource.hasId()) {
            resource.id = UUID.randomUUID().toString()
        } // generate new ID for new resources
        collection.add(context.newJsonParser().encodeResourceToString(resource)).execute()
        return resource.id
    }

    fun update(resource: T) {
        getDatabaseId(resource.id)?.let {
            collection.replaceOne(
                it, context.newJsonParser().encodeResourceToString(resource)
            )
        } ?: insert(resource) // add new resource if not found
    }

    fun delete(fhirId: String) {
        getDatabaseId(fhirId)?.let { collection.removeOne(it) }
    }

    fun findById(fhirId: String): T {
        val resourceJSON = findByIdQuery(fhirId)?.toString()
            ?: throw ResourceNotFoundException("No resource found with id: $fhirId")
        return context.newJsonParser().parseResource(
            resourceType, resourceJSON
        )
    }

    fun getAll(): List<T> {
        val list = mutableListOf<T>()
        val parser = context.newJsonParser()
        collection.find().execute().forEach {
            list.add(parser.parseResource(resourceType, it.toString()))
        }
        return list
    }

    private fun findByIdQuery(fhirId: String?): DbDoc? {
        if (fhirId == null) return null
        return collection.find("id = :id").bind("id", fhirId).execute().fetchOne()
    }

    private fun getDatabaseId(fhirId: String): String? {
        // "_id" is the MySQL-specific document identifier, which is different from the FHIR "id"
        return findByIdQuery(fhirId)?.let { (it["_id"] as JsonString).string }
    }
}
