package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.CarePlan
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class R4CarePlanDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<CarePlan>() {
    override var resourceType = CarePlan::class.java
    override var collection = AtomicReference(database.createCollection(CarePlan::class.simpleName, true))

    fun searchByQuery(
        subject: String? = null
    ): List<CarePlan> {

        // Build queryFragments into query conditions joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of Condition resources that match
        val parser = context.newJsonParser()
        return collection.get().find(query).execute().map { parser.parseResource(resourceType, it.toString()) }
    }
}
