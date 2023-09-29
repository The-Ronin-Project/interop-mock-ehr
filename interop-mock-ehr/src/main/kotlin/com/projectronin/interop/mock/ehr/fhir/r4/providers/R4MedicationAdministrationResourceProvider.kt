package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationAdministrationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.MedicationAdministration
import org.springframework.stereotype.Component

@Component
class R4MedicationAdministrationResourceProvider(override var resourceDAO: R4MedicationAdministrationDAO) :
    BaseResourceProvider<MedicationAdministration, R4MedicationAdministrationDAO>() {

    override fun getResourceType(): Class<out IBaseResource> {
        return MedicationAdministration::class.java
    }

    @Search
    fun search(@RequiredParam(name = MedicationAdministration.SP_REQUEST) referenceParam: ReferenceParam): List<MedicationAdministration> {
        return resourceDAO.searchByRequest(referenceParam.value)
    }
}
