package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component

@Component
class R4PatientDAO(database: Schema) : BaseResourceDAO<Patient>() {
    override var context: FhirContext = FhirContext.forR4()
    override var resourceType = Patient::class.java
    override var collection: Collection = database.createCollection(Patient::class.simpleName, true)

    fun searchByQuery(
        birthdate: String? = null,
        givenName: String? = null,
        familyName: String? = null,
        gender: String? = null,
        email: String? = null,
        telecom: ContactPoint? = null
    ): List<Patient> {
        val queryFragments = mutableListOf<String>()

        birthdate?.let { queryFragments.add("birthDate = '$it'") }
        gender?.let { queryFragments.add("gender = '$it'") }

        // name is surprisingly difficult to query, this isn't perfect
        givenName?.let { queryFragments.add("'$it' in name[*].given[*]") }
        familyName?.let { queryFragments.add("'$it' in name[*].family") }

        // it may be worth checking 'system' in the future for these, but this is fine for now
        email?.let { queryFragments.add("'$it' in telecom[*].value") }
        telecom?.let { queryFragments.add("'${it.value}' in telecom[*].value") }

        val query = queryFragments.joinToString(" AND ")
        val parser = context.newJsonParser()
        return collection.find(query).execute().map {
            parser.parseResource(resourceType, it.toString())
        }
    }

    fun searchByIdentifier(identifier: Identifier): Patient? {
        val parser = context.newJsonParser()
        // note that this query is a bit rigid and expects identifiers in database to only ever have just value and system
        val patientDbDoc =
            collection.find("{'value':'${identifier.value}','system':'${identifier.system}'} in identifier[*]")
                .execute().fetchOne()
        return patientDbDoc?.let { parser.parseResource(resourceType, it.toString()) }
    }
}
