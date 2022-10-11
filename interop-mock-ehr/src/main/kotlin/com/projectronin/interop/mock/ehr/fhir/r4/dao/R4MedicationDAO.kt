package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Medication
import org.springframework.stereotype.Component

@Component
class R4MedicationDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Medication>() {
    override var resourceType = Medication::class.java
    override var collection: Collection = database.createCollection(Medication::class.simpleName, true)
}
