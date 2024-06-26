package com.projectronin.interop.mock.ehr.epic.dal

import com.projectronin.interop.mock.ehr.epic.transform.R4AppointmentTransformer
import com.projectronin.interop.mock.ehr.epic.transform.R4CommunicationTransformer
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CommunicationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4EncounterDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4FlagDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationAdministrationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationRequestDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.springframework.stereotype.Component

@Component
class EpicDAL(
    val r4PatientDAO: R4PatientDAO,
    val r4AppointmentDAO: R4AppointmentDAO,
    val r4PractitionerDAO: R4PractitionerDAO,
    val r4AppointmentTransformer: R4AppointmentTransformer,
    val r4CommunicationDAO: R4CommunicationDAO,
    val r4CommunicationTransformer: R4CommunicationTransformer,
    val r4LocationDAO: R4LocationDAO,
    val r4EncounterDAO: R4EncounterDAO,
    val r4FlagDAO: R4FlagDAO,
    val r4MedAdminDAO: R4MedicationAdministrationDAO,
    val r4MedicationRequestDAO: R4MedicationRequestDAO,
)
