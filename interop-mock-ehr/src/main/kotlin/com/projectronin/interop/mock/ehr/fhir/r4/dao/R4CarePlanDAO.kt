package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.CarePlan
import org.springframework.stereotype.Component
import java.util.Date

@Component
class R4CarePlanDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<CarePlan>(context, schema, CarePlan::class.java) {
    fun searchByQuery(
        subject: String,
        category: String,
        fromDate: Date? = null,
        toDate: Date? = null,
    ): List<CarePlan> {
        // Build queryFragments into query care plans joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }
        category.let { queryFragments.add("('${it.escapeSQL()}') in category[*].coding[*].code") }

        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        val carePlans = mutableListOf<CarePlan>()

        // Run the query and return a List of Care Plan resources that match
        val parser = context.newJsonParser()
        schema.run(collection) {
            find(query).execute().forEach {
                carePlans.add(parser.parseResource(resourceType, it.toString()))
            }
        }

        val minDate = Date(Long.MIN_VALUE)
        val maxDate = Date(Long.MAX_VALUE)
        val searchStart = fromDate ?: minDate
        val searchEnd = toDate ?: maxDate

        return carePlans.filter { carePlan ->
            val planStart = carePlan.period.start ?: minDate
            val planEnd = carePlan.period.end ?: maxDate
            !(planStart.after(searchEnd) || planEnd.before(searchStart))
        }
    }
}
