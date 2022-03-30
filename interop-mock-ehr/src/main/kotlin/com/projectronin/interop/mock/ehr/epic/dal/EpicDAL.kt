package com.projectronin.interop.mock.ehr.epic.dal

import com.projectronin.interop.mock.ehr.epic.transform.R4AppointmentTransformer
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.springframework.stereotype.Component

@Component
class EpicDAL(
    val r4PatientDAO: R4PatientDAO,
    val r4AppointmentDAO: R4AppointmentDAO,
    val r4PractitionerDAO: R4PractitionerDAO,
    val r4AppointmentTransformer: R4AppointmentTransformer
)
