package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.Appointment as R4Appointment

@Component
class R4AppointmentTransformer(private val r4PractitionerDAO: R4PractitionerDAO) {

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
        val providers =
            r4Appointment.participant.filter { it.actor.reference.contains("Practitioner") }.map { practitionerRef ->
                val practitioner = kotlin.runCatching {
                    r4PractitionerDAO.findById(practitionerRef.actor.reference.removePrefix("Practitioner/"))
                }.getOrNull()

                // check actual R4 practitioner for internal ID
                val identifierVal = practitioner?.identifier?.find { it.system == "mockEHRProviderSystem" }?.value
                    // then check if an identifier exists on the reference (unlikely)
                    ?: practitionerRef.actor.identifier.takeIf { it.system == "mockEHRProviderSystem" }?.value

                ScheduleProviderReturnWithTime(
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(identifierVal ?: "NO-INTERNAL-ID", "external")
                    ),
                    providerName = "",
                    time = "",
                    departmentIDs = emptyList()
                )
            }

        return EpicAppointment(
            appointmentDuration = r4Appointment.minutesDuration.toString(),
            appointmentNotes = listOf(r4Appointment.patientInstruction ?: "", r4Appointment.comment ?: ""),
            appointmentStartTime = SimpleDateFormat("hh:mm aa").format(r4Appointment.start),
            appointmentStatus = r4Appointment.status.toCode(),
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
