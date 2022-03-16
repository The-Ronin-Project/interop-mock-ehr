package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner
import org.springframework.stereotype.Component

@Component
class R4PractitionerDAO(database: Schema) : BaseResourceDAO<Practitioner>() {
    override var context: FhirContext = FhirContext.forR4()
    override var resourceType = Practitioner::class.java
    override var collection: Collection = database.createCollection(Practitioner::class.simpleName, true)

    fun searchByIdentifier(identifier: Identifier): Practitioner? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val practitionerDbDoc =
            collection.find("{'value':'${identifier.value}','system':'${identifier.system}'} in identifier[*]")
                .execute().fetchOne()
        return practitionerDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }
}
