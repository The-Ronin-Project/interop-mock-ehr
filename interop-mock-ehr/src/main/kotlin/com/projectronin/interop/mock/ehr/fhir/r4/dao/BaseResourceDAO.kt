package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.DbDoc
import com.mysql.cj.xdevapi.JsonString
import org.hl7.fhir.r4.model.Resource
import java.util.UUID

abstract class BaseResourceDAO<T : Resource> {

    abstract var context: FhirContext
    abstract var resourceType: Class<T>
    abstract var collection: Collection

    fun insert(resource: Resource): String {
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

    fun patch(fhirId: String, rawPatch: String) {
        val mapper = ObjectMapper()
        val patch = mapper.readValue(rawPatch, JsonPatch::class.java)
        val resourceJSON = findByIdQuery(fhirId)?.toString()
        val patched = patch.apply(mapper.readTree(resourceJSON)).toString()
        // deserialize first to make sure patched data isn't invalid
        update(context.newJsonParser().parseResource(resourceType, patched))
    }

    private fun findByIdQuery(fhirId: String?): DbDoc? {
        if (fhirId == null) return null
        return collection.find("id = :id")
            .bind("id", fhirId.removePrefix("${resourceType.simpleName}/"))
            .execute()
            .fetchOne()
    }

    private fun getDatabaseId(fhirId: String): String? {
        // "_id" is the MySQL-specific document identifier, which is different from the FHIR "id"
        return findByIdQuery(fhirId)?.let { (it["_id"] as JsonString).string }
    }

    /**
     * Input is a comma-separated list of FHIR tokens as system|code or |code or system| or code alone.
     * Returns a search string suitable as part of a where clause in a query to mock EHR.
     * Returns null if no search string can be formed from the input.
     */
    fun getSearchStringForFHIRTokens(fhirTokens: TokenOrListParam? = null): String? {
        if (fhirTokens == null) {
            return null
        }
        val queryFragments = mutableListOf<String>()
        val categories = fhirTokens.valuesAsQueryTokens
        val phraseList = categories.mapNotNull { token ->
            getSearchStringForFHIRToken(token)
        }
        if (phraseList.isNotEmpty()) {
            queryFragments.add(" ( ")
            queryFragments.add(phraseList.joinToString(" OR "))
            queryFragments.add(" ) ")
            return queryFragments.joinToString("")
        }
        return null
    }

    /**
     * Input is a FHIR token as system|code or |code or system| or code alone.
     * Returns a search string suitable as part of a where clause in a query to mock EHR.
     * Returns null if no search string can be formed from the input.
     */
    fun getSearchStringForFHIRToken(fhirToken: TokenParam? = null): String? {
        if (fhirToken == null) {
            return null
        }
        val system = fhirToken.system
        val code = fhirToken.value
        return if (system.isNotEmpty()) {
            if (code.isNotEmpty()) {
                "('$system' in category[*].coding[*].system AND '$code' in category[*].coding[*].code)"
            } else {
                "('$system' in category[*].coding[*].system)"
            }
        } else {
            if (code.isNotEmpty()) {
                "('$code' in category[*].coding[*].code OR '$code' in category[*].text)"
            } else {
                null
            }
        }
    }
}
