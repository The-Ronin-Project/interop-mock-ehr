package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Medication
import org.springframework.stereotype.Component

@Component
class R4MedicationDAO(schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Medication>(context, schema, Medication::class.java)
