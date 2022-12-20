package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.CareTeam
import org.springframework.stereotype.Component

@Component
class R4CareTeamDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<CareTeam>() {
    override var resourceType = CareTeam::class.java
    override var collection: Collection = database.createCollection(CareTeam::class.simpleName, true)

    /**
     * Finds CareTeams based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param subject string for filtering CareTeam.subject.reference values.
     * @param status string for filtering CareTeam.status, a code string.
     */
    fun searchByQuery(
        subject: String? = null,
        status: String? = null,
    ): List<CareTeam> {
        // Build queryFragments into query joined with 'AND'
        val queryFragments = mutableListOf<String>()
        subject?.let { queryFragments.add("('$it' = subject.reference)") }
        status?.let { queryFragments.add("('$it' = status)") }
        if (queryFragments.isEmpty()) return listOf()
        val query = queryFragments.joinToString(" AND ")

        // Run the query and return a List of resources that match
        val parser = context.newJsonParser()
        return collection.find(query).execute().mapNotNull { parser.parseResource(resourceType, it.toString()) }
    }
}
