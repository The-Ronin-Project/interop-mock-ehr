package com.projectronin.interop.mock.ehr.fhir.stu3.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationStatementDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.toDSTU3
import org.hl7.fhir.dstu3.model.MedicationStatement
import org.hl7.fhir.instance.model.api.IBaseResource
import org.springframework.stereotype.Component
import org.hl7.fhir.r4.model.MedicationStatement as R4MedicationStatement

@Component
class STU3MedicationStatementProvider(override var resourceDAO: R4MedicationStatementDAO) :
    STU3BaseResourceProvider<MedicationStatement, R4MedicationStatement, R4MedicationStatementDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationStatement::class.java
    }

    @Search
    fun search(
        @RequiredParam(name = MedicationStatement.SP_PATIENT) patientReferenceParam: ReferenceParam,
    ): List<MedicationStatement> {
        val subject = patientReferenceParam.let { "Patient/${it.value}" }
        return resourceDAO.searchByQuery(subject).map { it.toDSTU3() }
    }
}
