package com.projectronin.interop.mock.ehr.hl7v2.converters

import ca.uhn.hl7v2.model.v251.datatype.CX
import org.hl7.fhir.r4.model.Identifier

fun CX.toIdentifier(): Identifier {
    val identifier = Identifier()
    identifier.value = this.idNumber.value
    identifier.system = this.assigningAuthority.namespaceID.value
    return identifier
}
