package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class R4PractitionerRoleDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<PractitionerRole>() {
    override var resourceType = PractitionerRole::class.java
    override var collection = AtomicReference(database.createCollection(PractitionerRole::class.simpleName, true))

    fun searchByIdentifier(identifier: Identifier): PractitionerRole? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val practitionerDbDoc =
            collection.get()
                .find("{'value':'${identifier.value}','system':'${identifier.system}'} in identifier[*]")
                .execute()
                .fetchOne()
        return practitionerDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }

    fun searchByQuery(
        locationReferences: List<Reference> = listOf(),
        practitionerReference: Reference? = null
    ): List<PractitionerRole> {
        val parser = context.newJsonParser()
        val roleList = mutableListOf<PractitionerRole>()
        val queryFragments = mutableListOf<String>()

        locationReferences.forEach { ref ->
            ref.reference?.let { queryFragments.add("'$it' in location[*].reference") }
        }
        practitionerReference?.reference?.let { queryFragments.add("'$it' in practitioner.reference") }

        val query = queryFragments.joinToString(" AND ")
        collection.get().find(query).execute().forEach {
            roleList.add(parser.parseResource(resourceType, it.toString()))
        }

        return roleList
    }
}
