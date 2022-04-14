package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.mock.ehr.epic.dal.EpicDAL
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.text.SimpleDateFormat
import java.util.UUID
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as EpicAppointment

@RestController
@RequestMapping("/epic")
class EpicServer(private var dal: EpicDAL) {

    @RequestMapping("/oauth2/token")
    fun getAuthToken(): EpicAuthentication {
        return EpicAuthentication(
            accessToken = UUID.randomUUID().toString(),
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search"
        )
    }

    @RequestMapping("/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments")
    fun getAppointmentsByPatient(@RequestBody request: GetPatientAppointmentsRequest): GetAppointmentsResponse {
        // start date required
        val start = kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.startDate) }
            .getOrElse { return errorResponse("INVALID-START-DATE") }

        // end date not required
        val end = request.endDate?.let {
            kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.endDate) }
                .getOrElse { return errorResponse("INVALID-END-DATE") }
        }
        // more validation
        end?.let { if (start.after(it)) return errorResponse("END-DATE-BEFORE-START-DATE") }
        if (request.userID == null) return errorResponse("NO-USER-FOUND") // can check this 'for real' later

        // try to find patient
        val patientID = Identifier().setValue(request.patientId).setSystem("MRN")
        val patient = dal.r4PatientDAO.searchByIdentifier(patientID) ?: return errorResponse("NO-PATIENT-FOUND")

        // search for appointments
        val r4appointments = dal.r4AppointmentDAO.searchByQuery(
            references = listOf(Reference().setReference(patient.id)),
            fromDate = start,
            toDate = end
        )
        val epicAppointments =
            r4appointments.map { dal.r4AppointmentTransformer.transformToEpicAppointment(it, patient) }
        return GetAppointmentsResponse(appointments = epicAppointments, error = null)
    }

    @RequestMapping("/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments")
    fun getAppointmentsByPractitioner(@RequestBody request: GetProviderAppointmentRequest): GetAppointmentsResponse {

        // start date required
        val start = kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.startDate) }
            .getOrElse { return errorResponse("INVALID-START-DATE") }

        // end date not required
        val end = request.endDate?.let {
            kotlin.runCatching { SimpleDateFormat("MM/dd/yyyy").parse(request.endDate) }
                .getOrElse { return errorResponse("INVALID-END-DATE") }
        }

        // more validation
        end?.let { if (start.after(it)) return errorResponse("END-DATE-BEFORE-START-DATE") }
        if (request.userID == null) return errorResponse("NO-USER-FOUND")

        // find practitioners
        val r4Practitioners = request.providers?.mapNotNull {
            dal.r4PractitionerDAO.searchByIdentifier(Identifier().setValue(it.id).setSystem(it.idType))
        } ?: return errorResponse("NO-PROVIDER-FOUND")
        if (r4Practitioners.isEmpty()) return errorResponse("NO-PROVIDER-FOUND")

        // find all appointments for all practitioners
        val epicAppointments = mutableListOf<EpicAppointment>()
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

    @RequestMapping("/api/epic/2014/Common/Utility/SENDMESSAGE/Message")
    fun createCommunication(@RequestBody sendMessageRequest: SendMessageRequest): SendMessageResponse {
        val communication = dal.r4CommunicationTransformer.transformFromSendMessage(sendMessageRequest)
        val newCommunicationId = dal.r4CommunicationDAO.insert(communication)
        return SendMessageResponse(listOf(IDType(id = newCommunicationId, type = "FHIR ID")))
    }
}

// wrapper for throwing 400 - Bad Request responses
private fun errorResponse(msg: String): GetAppointmentsResponse {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
}
