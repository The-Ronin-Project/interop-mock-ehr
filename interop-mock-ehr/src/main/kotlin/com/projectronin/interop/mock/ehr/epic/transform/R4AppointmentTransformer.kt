package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Appointment as R4Appointment

@Component
class R4AppointmentTransformer(
    private val r4PractitionerDAO: R4PractitionerDAO,
    private val r4LocationDAO: R4LocationDAO
) {

    fun transformToEpicAppointment(r4Appointment: R4Appointment, r4Patient: Patient?): EpicAppointment {
        val patientIDs = r4Patient?.identifier?.map {
            // mimic Epic for MRNs and Internal IDs
            val system = when (it.system) {
                "mockEHRMRNSystem" -> "MRN"
                "mockPatientInternalSystem" -> "Internal"
                else -> it.system
            }
            IDType(it.value, system)
        }

        val departmentList =
            r4Appointment.participant.find { it.actor.reference.contains("Location") }?.let { locationRef ->
                val location =
                    runCatching {
                        r4LocationDAO.findById(locationRef.actor.reference.removePrefix("Location/"))
                    }.getOrNull()
                val depIdentifier =
                    location?.identifier?.find { it.system == "mockEHRDepartmentInternalSystem" }?.value
                listOf(IDType(depIdentifier ?: "NO-INTERNAL-ID", "Internal"))
            } ?: emptyList()

        val providers =
            r4Appointment.participant.filter { it.actor.reference.contains("Practitioner") }.map { practitionerRef ->
                val practitioner = runCatching {
                    r4PractitionerDAO.findById(practitionerRef.actor.reference.removePrefix("Practitioner/"))
                }.getOrNull()

                // check actual R4 practitioner for internal ID
                val identifierVal = practitioner?.identifier?.find { it.system == "mockEHRProviderSystem" }?.value
                    ?: practitionerRef.actor.identifier.takeIf { it.system == "mockEHRProviderSystem" }?.value

                ScheduleProviderReturnWithTime(
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(identifierVal ?: "NO-INTERNAL-ID", "external")
                    ),
                    providerName = "",
                    time = "",
                    departmentIDs = departmentList
                )
            }

        return EpicAppointment(
            appointmentDuration = r4Appointment.minutesDuration.toString(),
            appointmentNotes = listOf(r4Appointment.patientInstruction ?: "", r4Appointment.comment ?: ""),
            appointmentStartTime = SimpleDateFormat("hh:mm aa").format(r4Appointment.start),
            appointmentStatus = transformAppointmentStatus(r4Appointment.status.toCode()),
            contactIDs = listOf(IDType(r4Appointment.id.removePrefix("Appointment/"), "CSN")),
            date = SimpleDateFormat("MM/dd/yyyy").format(r4Appointment.start),
            patientIDs = patientIDs ?: listOf(),
            patientName = r4Patient?.name?.find { it.use.toCode() == "usual" }?.nameAsSingleString
                ?: r4Patient?.nameFirstRep?.nameAsSingleString ?: "",
            providers = providers,
            visitTypeName = r4Appointment.appointmentType.text ?: ""
        )
    }
}

fun transformAppointmentStatus(r4StatusCode: String): String {
    return when (r4StatusCode) {
        "booked" -> "Scheduled"
        "pending" -> "Scheduled"
        "noshow" -> "No Show"
        "arrived" -> "Arrived"
        "fulfilled" -> "Completed"
        "checked-in" -> "Arrived"
        else -> "?"
    }
}
