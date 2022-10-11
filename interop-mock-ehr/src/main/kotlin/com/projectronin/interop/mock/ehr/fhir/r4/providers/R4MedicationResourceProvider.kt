package com.projectronin.interop.mock.ehr.fhir.r4.providers

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Medication
import org.springframework.stereotype.Component

@Component
class R4MedicationResourceProvider(override var resourceDAO: R4MedicationDAO) :
    BaseResourceProvider<Medication, R4MedicationDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return Medication::class.java
    }
}
