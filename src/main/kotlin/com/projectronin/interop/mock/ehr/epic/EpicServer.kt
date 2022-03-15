package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.mock.ehr.epic.dal.EpicDAL
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.util.UUID

@RestController
@RequestMapping("/epic")
class EpicServer(private var dal: EpicDAL) {

    @GetMapping("/oauth2/token")
    fun getAuthToken(): EpicAuthentication {
        return EpicAuthentication(
            accessToken = UUID.randomUUID().toString(),
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search"
        )
    }

    @GetMapping("/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments")
    fun getAppointmentsByPatient(@RequestBody request: GetPatientAppointmentsRequest): GetAppointmentsResponse {
        val start = SimpleDateFormat("dd/MM/yyyy").parse(request.startDate)
        val end = SimpleDateFormat("dd/MM/yyyy").parse(request.endDate)
        val patientID = Identifier().setValue(request.patientId).setSystem("MRN")
        val patient = dal.r4PatientDAO.searchByIdentifier(patientID) ?: return GetAppointmentsResponse(
            listOf(),
            "No patient found."
        )
        val r4appointments = dal.r4AppointmentDAO.searchByQuery(
            references = listOf(Reference().setReference(patient.id)),
            fromDate = start,
            toDate = end
        )
        val epicAppointments =
            r4appointments.map { dal.r4AppointmentTransformer.transformToEpicAppointment(it, patient) }
        return GetAppointmentsResponse(appointments = epicAppointments, error = "No error good job")
    }
}
