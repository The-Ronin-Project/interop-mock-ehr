package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Binary
import org.springframework.stereotype.Component

@Component
class R4BinaryDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Binary>() {
    override var resourceType = Binary::class.java
    override var collection: Collection = database.createCollection(Binary::class.simpleName, true)
}
