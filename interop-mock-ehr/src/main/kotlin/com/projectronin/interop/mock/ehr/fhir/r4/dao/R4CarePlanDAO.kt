package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.CarePlan
import org.springframework.stereotype.Component

@Component
class R4CarePlanDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<CarePlan>(context, schema, CarePlan::class.java) {
    fun searchByQuery(
        subject: String? = null
    ): List<CarePlan> {
        // Build queryFragments into query conditions joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of Condition resources that match
        val parser = context.newJsonParser()
        return schema.run(collection) {
            find(query).execute().map { parser.parseResource(resourceType, it.toString()) }
        }
    }
}
