package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

@Component
class R4PractitionerRoleDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<PractitionerRole>(context, schema, PractitionerRole::class.java) {
    fun searchByIdentifier(identifier: Identifier): PractitionerRole? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val practitionerDbDoc =
            schema.run(collection) {
                find("{'value':'${identifier.value}','system':'${identifier.system}'} in identifier[*]")
                    .execute()
                    .fetchOne()
            }
        return practitionerDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }

    fun searchByQuery(
        locationReferences: List<Reference> = listOf(),
        practitionerReference: Reference? = null,
    ): List<PractitionerRole> {
        val parser = context.newJsonParser()
        val roleList = mutableListOf<PractitionerRole>()
        val queryFragments = mutableListOf<String>()

        locationReferences.forEach { ref ->
            ref.reference?.let { queryFragments.add("'${it.escapeSQL()}' in location[*].reference") }
        }
        practitionerReference?.reference?.let { queryFragments.add("'${it.escapeSQL()}' in practitioner.reference") }

        val query = queryFragments.joinToString(" AND ")
        schema.run(collection) {
            find(query).execute().forEach {
                roleList.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        return roleList
    }
}
