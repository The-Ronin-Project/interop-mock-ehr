package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.mysql.cj.xdevapi.DbDoc
import com.mysql.cj.xdevapi.JsonString
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Resource
import java.util.UUID

abstract class BaseResourceDAO<T : Resource>(
    protected val context: FhirContext,
    private val schema: SafeXDev,
    val resourceType: Class<T>
) {
    protected val collection: SafeXDev.SafeCollection = schema.createCollection(resourceType)

    fun insert(resource: Resource): String {
        if (!resource.hasId()) {
            resource.id = UUID.randomUUID().toString()
        } // generate new ID for new resources
        schema.run(collection) {
            add(context.newJsonParser().encodeResourceToString(resource)).execute()
        }
        return resource.id
    }

    fun update(resource: T) {
        getDatabaseId(resource.id)?.let {
            schema.run(collection) {
                replaceOne(
                    it,
                    context.newJsonParser().encodeResourceToString(resource)
                )
            }
        } ?: insert(resource) // add new resource if not found
    }

    fun delete(fhirId: String) {
        getDatabaseId(fhirId)?.let { schema.run(collection) { removeOne(it) } }
    }

    fun findById(fhirId: String): T {
        val resourceJSON = findByIdQuery(fhirId)?.toString()
            ?: throw ResourceNotFoundException("No resource found with id: $fhirId")
        return context.newJsonParser().parseResource(
            resourceType,
            resourceJSON
        )
    }

    fun getAll(): List<T> {
        val list = mutableListOf<T>()
        val parser = context.newJsonParser()
        schema.run(collection) {
            find().execute().forEach {
                list.add(parser.parseResource(resourceType, it.toString()))
            }
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
        return schema.run(collection) {
            find("id = :id")
                .bind("id", fhirId.removePrefix("${resourceType.simpleName}/"))
                .execute()
                .fetchOne()
        }
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
    fun getSearchStringForFHIRTokens(fhirTokens: TokenOrListParam? = null, fieldName: String? = "category"): String? {
        if (fhirTokens == null) {
            return null
        }
        val queryFragments = mutableListOf<String>()
        val categories = fhirTokens.valuesAsQueryTokens
        val phraseList = categories.mapNotNull { token ->
            getSearchStringForFHIRToken(token, fieldName)
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
    fun getSearchStringForFHIRToken(fhirToken: TokenParam? = null, fieldName: String? = "category"): String? {
        if (fhirToken == null) {
            return null
        }
        val system = fhirToken.system
        val code = fhirToken.value
        return if (!system.isNullOrEmpty()) {
            if (!code.isNullOrEmpty()) {
                "('${system.escapeSQL()}' in $fieldName[*].coding[*].system AND '${code.escapeSQL()}' in $fieldName[*].coding[*].code)"
            } else {
                "('${system.escapeSQL()}' in $fieldName[*].coding[*].system)"
            }
        } else {
            if (!code.isNullOrEmpty()) {
                "('${code.escapeSQL()}' in $fieldName[*].coding[*].code OR '${code.escapeSQL()}' in $fieldName[*].text)"
            } else {
                null
            }
        }
    }
}
