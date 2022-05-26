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
        /*
         grabs all potential practitioners to do further searching
         this query could potentially be made into the actual search string,
         but the logic ends up being quite complex and hard to read
         better to reasonably filter potential results from the db and then further filter them
         in easier to understand code
         */
        val searchString = "'${identifier.value}' in $.identifier[*].value"
        val practitionerDbDoc =
            collection.find(searchString).execute().fetchAll()
        val practitioners = practitionerDbDoc.mapNotNull { parser.parseResource(resourceType, it.toString()) }
        return practitioners.singleOrNull { practitioner ->
            practitioner.identifier.any {
                it.value == identifier.value &&
                    // either not specified in search or matches
                    (identifier.system == null || it.system == identifier.system) &&
                    (identifier.type.text == null || it.type.text == identifier.type.text)
            }
        }
    }
}
