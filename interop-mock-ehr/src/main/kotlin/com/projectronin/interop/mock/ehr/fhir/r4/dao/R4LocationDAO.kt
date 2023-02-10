package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class R4LocationDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Location>() {
    override var resourceType = Location::class.java
    override var collection = AtomicReference(database.createCollection(Location::class.simpleName, true))

    fun searchByIdentifier(identifier: Identifier): Location? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val locationDbDoc =
            collection.get().find("{'value':'${identifier.value}','system':'${identifier.system}'} in identifier[*]")
                .execute().fetchOne()
        return locationDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }
}
