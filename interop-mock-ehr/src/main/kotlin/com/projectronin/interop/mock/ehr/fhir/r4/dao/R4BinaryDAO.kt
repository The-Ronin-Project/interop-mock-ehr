package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import org.hl7.fhir.r4.model.Binary
import org.springframework.stereotype.Component

@Component
class R4BinaryDAO(schema: SafeXDev, context: FhirContext) :
    BaseResourceDAO<Binary>(context, schema, Binary::class.java)
