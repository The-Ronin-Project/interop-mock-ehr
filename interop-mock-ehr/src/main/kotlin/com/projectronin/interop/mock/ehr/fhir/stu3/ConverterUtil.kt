package com.projectronin.interop.mock.ehr.fhir.stu3

import org.hl7.fhir.convertors.factory.VersionConvertorFactory_30_40
import org.hl7.fhir.dstu3.model.Resource

// import org.hl7.fhir.dstu3.model.Type

@Suppress("UNCHECKED_CAST")
fun <DSTU3 : Resource> org.hl7.fhir.r4.model.Resource.toDSTU3(): DSTU3 {
    return VersionConvertorFactory_30_40.convertResource(this) as DSTU3
}

@Suppress("UNCHECKED_CAST")
fun <R4 : org.hl7.fhir.r4.model.Resource> Resource.toR4(): R4 {
    return VersionConvertorFactory_30_40.convertResource(this) as R4
}

// leaving these here in case we need them in the future
// @Suppress("UNCHECKED_CAST")
// fun <DSTU3 : org.hl7.fhir.r4.model.Type> Type.toDSTU3() : DSTU3 {
//     return VersionConvertorFactory_30_40.convertType(this) as DSTU3
// }
// @Suppress("UNCHECKED_CAST")
// fun <R4 : org.hl7.fhir.r4.model.Type> Type.toR4() : R4 {
//     return VersionConvertorFactory_30_40.convertType(this) as R4
// }
