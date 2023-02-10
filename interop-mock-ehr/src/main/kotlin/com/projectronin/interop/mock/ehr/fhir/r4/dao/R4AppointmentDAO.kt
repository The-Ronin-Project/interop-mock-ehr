package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

@Component
class R4AppointmentDAO(database: Schema, override var context: FhirContext) : BaseResourceDAO<Appointment>() {
    override var resourceType = Appointment::class.java
    override var collection = AtomicReference(database.createCollection(Appointment::class.simpleName, true))

    /**
     * Finds appointments based on input query parameters. Treats all inputs as a logical 'AND'.
     * @param references any number of FHIR-style References, i.e. 'Patient/123'
     * @param fromDate the earliest date to start searching for appointments
     * @param toDate the latest date to be searching for appointments
     */
    fun searchByQuery(
        references: List<Reference> = listOf(),
        fromDate: Date? = null,
        toDate: Date? = null,
        status: String? = null
    ): List<Appointment> {
        val queryFragments = mutableListOf<String>()

        references.forEach { ref ->
            ref.reference?.let { queryFragments.add("'$it' in participant[*].actor.reference") }
        }
        status?.let { queryFragments.add("'$it' in status") }

        val query = queryFragments.joinToString(" AND ")

        val apptList = mutableListOf<Appointment>()
        val parser = context.newJsonParser()

        collection.get().find(query).execute().forEach {
            apptList.add(parser.parseResource(resourceType, it.toString()))
        }

        // no good way to compare dates in the query string, so we have to filter post-query.
        return apptList.filter { appointment ->
            (toDate?.let { appointment.start?.before(it) ?: false } ?: true) && // before upper bound?
                (fromDate?.let { appointment.start?.after(it) ?: false } ?: true) // and after lower bound?
        }
    }
}
