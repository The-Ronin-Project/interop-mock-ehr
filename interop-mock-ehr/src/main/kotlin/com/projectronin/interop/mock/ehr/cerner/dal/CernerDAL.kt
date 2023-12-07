package com.projectronin.interop.mock.ehr.cerner.dal

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CommunicationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4EncounterDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.springframework.stereotype.Component

@Component
class CernerDAL(
    val r4PatientDAO: R4PatientDAO,
    val r4AppointmentDAO: R4AppointmentDAO,
    val r4PractitionerDAO: R4PractitionerDAO,
    val r4CommunicationDAO: R4CommunicationDAO,
    val r4LocationDAO: R4LocationDAO,
    val r4EncounterDAO: R4EncounterDAO,
)
