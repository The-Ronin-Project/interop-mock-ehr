package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
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
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as EpicAppointment

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
        return GetAppointmentsResponse(appointments = epicAppointments, error = null)
    }

    @GetMapping("/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments")
    fun getAppointmentsByPractitioner(@RequestBody request: GetProviderAppointmentRequest): GetAppointmentsResponse {
        val epicAppointments = mutableListOf<EpicAppointment>()
        val start = SimpleDateFormat("dd/MM/yyyy").parse(request.startDate)
        val end = SimpleDateFormat("dd/MM/yyyy").parse(request.endDate)
        val r4Practitioners = request.providers.mapNotNull {
            dal.r4PractitionerDAO.searchByIdentifier(Identifier().setValue(it.id).setSystem(it.idType))
        }

        if (r4Practitioners.isEmpty()) return GetAppointmentsResponse(
            listOf(),
            "No practitioners found matching request."
        )

        r4Practitioners.forEach {
            val r4Appointments = dal.r4AppointmentDAO.searchByQuery(
                references = listOf(Reference().setReference(it.id)),
                fromDate = start,
                toDate = end
            )

            r4Appointments.forEach { r4Appointment ->
                val patientId =
                    r4Appointment.participant.find { participant ->
                        participant.actor.reference.contains("Patient")
                    }?.actor?.reference

                val patient = patientId?.let { dal.r4PatientDAO.findById(patientId.removePrefix("Patient/")) }

                epicAppointments.add(dal.r4AppointmentTransformer.transformToEpicAppointment(r4Appointment, patient))
            }
        }
        return GetAppointmentsResponse(appointments = epicAppointments, error = null)
    }
}
