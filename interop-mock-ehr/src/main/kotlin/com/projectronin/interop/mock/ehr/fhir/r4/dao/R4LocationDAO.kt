package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.springframework.stereotype.Component

@Component
class R4LocationDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Location>(context, schema, Location::class.java) {

    fun searchByIdentifier(identifier: Identifier): Location? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val locationDbDoc =
            schema.run(collection) {
                find("{'value':'${identifier.value.escapeSQL()}','system':'${identifier.system.escapeSQL()}'} in identifier[*]")
                    .execute().fetchOne()
            }
        return locationDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }
}
