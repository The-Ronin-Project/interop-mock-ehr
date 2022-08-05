package com.projectronin.interop.mock.ehr.fhir.stu3.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import org.hl7.fhir.dstu3.model.ContactPoint
import org.hl7.fhir.dstu3.model.Identifier
import org.hl7.fhir.dstu3.model.Patient
import org.springframework.stereotype.Component

@Component
class STU3PatientDAO(database: Schema) : STU3BaseResourceDAO<Patient>() {
    override var context: FhirContext = FhirContext.forDstu3()
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
        /*
         grabs all potential patients to do further searching
         this query could potentially be made into the actual search string,
         but the logic ends up being quite complex and hard to read
         better to reasonably filter potential results from the db and then further filter them
         in easier to understand code
         */
        val searchString = "'${identifier.value}' in $.identifier[*].value"
        val patientDbDoc =
            collection.find(searchString).execute().fetchAll()
        val patients = patientDbDoc.mapNotNull { parser.parseResource(resourceType, it.toString()) }
        return patients.singleOrNull { patient ->
            patient.identifier.any {
                it.value == identifier.value &&
                    // either not specified in search or matches
                    (identifier.system == null || it.system == identifier.system) &&
                    (identifier.type.text == null || it.type.text == identifier.type.text)
            }
        }
    }
}
