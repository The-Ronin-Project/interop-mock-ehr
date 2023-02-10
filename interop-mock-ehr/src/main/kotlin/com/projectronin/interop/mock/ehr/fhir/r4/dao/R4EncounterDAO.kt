package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

@Component
class R4EncounterDAO(
    database: Schema,
    override var context: FhirContext
) : BaseResourceDAO<Encounter>() {
    override var resourceType = Encounter::class.java
    override var collection = AtomicReference(database.createCollection(Encounter::class.simpleName, true))

    /**
     * Finds Encounter based on input query parameters.
     * @param reference the patientFhirId for the ENCOUNTERS, i.e. 'Patient/eJcZl4T3ut30rE-k0LLiYyw3'
     * @param fromDate the earliest date to start searching for ENCOUNTERS
     * @param toDate the latest date to be searching for ENCOUNTERS
     */
    fun searchByQuery(
        reference: List<Reference> = listOf(),
        fromDate: Date? = null,
        toDate: Date? = null,
    ): List<Encounter> {
        val queryFragments = mutableListOf<String>()

        reference.forEach { ref ->
            ref.reference?.let { queryFragments.add("'$it' = subject.reference") }
        }

        val query = queryFragments.joinToString(" AND ")
        val encounterList = mutableListOf<Encounter>()
        val parser = context.newJsonParser()

        // run query, return list
        collection.get().find(query).execute().forEach {
            encounterList.add(parser.parseResource(resourceType, it.toString()))
        }

        return encounterList.filter { encounter ->
            (toDate?.let { encounter.period?.start?.before(it) ?: false } ?: true) &&
                (fromDate?.let { encounter.period?.start?.after(it) ?: false } ?: true)
        }
    }
}
