package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.util.escapeSQL
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Flag
import org.springframework.stereotype.Component

@Component
class R4FlagDAO(private val schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Flag>(context, schema, Flag::class.java) {

    fun searchByQuery(
        subject: String
    ): List<Flag> {
        val query = "('${subject.escapeSQL()}' = subject.reference)"

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return schema.run(collection) {
            find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
        }
    }
}
