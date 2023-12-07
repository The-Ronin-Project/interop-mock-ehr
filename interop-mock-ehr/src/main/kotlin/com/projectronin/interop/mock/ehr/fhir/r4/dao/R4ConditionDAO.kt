package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Condition
import org.springframework.stereotype.Component

@Component
class R4ConditionDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Condition>(context, schema, Condition::class.java) {
    /**
     * Finds conditions based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering Condition.subject.reference values.
     * @param category string for filtering Condition.category[*].coding[*].code or .category[*].text.
     * @param clinicalStatus string for filtering Condition.clinicalStatus.coding[*].code or .clinicalStatus.text.
     */
    fun searchByQuery(
        subject: String? = null,
        category: TokenOrListParam? = null,
        clinicalStatus: String? = null,
    ): List<Condition> {
        // Build queryFragments into query conditions joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('${it.escapeSQL()}' = subject.reference)") }
        category?.let { catList ->
            val phrase = getSearchStringForFHIRTokens(catList)
            if (!phrase.isNullOrEmpty()) {
                queryFragments.add(phrase)
            }
        }
        clinicalStatus?.let {
            queryFragments.add("('${it.escapeSQL()}' in clinicalStatus.coding[*].code OR '${it.escapeSQL()}' in clinicalStatus.text)")
        }

        // Join query conditions with 'AND'
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of Condition resources that match
        val parser = context.newJsonParser()
        return schema.run(collection) {
            find(query).execute().map { parser.parseResource(resourceType, it.toString()) }
        }
    }
}
