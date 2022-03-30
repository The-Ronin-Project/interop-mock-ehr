package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component

@Component
class R4PatientDAO(database: Schema) : BaseResourceDAO<Patient>() {
    override var context: FhirContext = FhirContext.forR4()
    override var resourceType = Patient::class.java
    override var collection: Collection = database.createCollection(Patient::class.simpleName, true)

    fun searchByBirthdate(birthdate: String): List<Patient> {
        val list = mutableListOf<Patient>()
        val parser = context.newJsonParser()
        collection.find("birthDate = :bd").bind("bd", birthdate).execute().forEach {
            list.add(parser.parseResource(resourceType, it.toString()))
        }
        return list
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
