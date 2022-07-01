package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.DocumentReference
import org.springframework.stereotype.Component

@Component
class R4DocumentReferenceDAO(database: Schema) : BaseResourceDAO<DocumentReference>() {
    override var context: FhirContext = FhirContext.forR4()
    override var resourceType = DocumentReference::class.java
    override var collection: Collection = database.createCollection(DocumentReference::class.simpleName, true)

    fun searchByQuery(
        subject: String? = null,
        category: String? = null,
        docStatus: String? = null,
        encounter: String? = null
    ): List<DocumentReference> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        docStatus?.let { queryFragments.add("('$it' = docStatus)") }
        encounter?.let { queryFragments.add("('$it' in context.encounter[*].reference)") }
        category?.let { queryFragments.add("('$it' in category[*].coding[*].code OR '$it' in category[*].text)") }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().map { parser.parseResource(resourceType, it.toString()) }
    }
}
