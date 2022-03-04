package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.fhir.BaseResourceDAO
import org.hl7.fhir.r4.model.Patient

class R4PatientDAO : BaseResourceDAO<Patient>() {
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
}
