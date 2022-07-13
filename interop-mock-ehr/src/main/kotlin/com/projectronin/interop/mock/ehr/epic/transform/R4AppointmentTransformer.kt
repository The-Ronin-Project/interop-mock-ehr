package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as EpicAppointment
import org.hl7.fhir.r4.model.Appointment as R4Appointment

@Component
class R4AppointmentTransformer {

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
        val providers = mutableListOf<ScheduleProviderReturnWithTime>()

        // in the future, we may want to link this concept to 'actual' Practitioner resources.
        r4Appointment.participant.filter { it.actor.reference.contains("Practitioner") }.forEach { practitioner ->
            providers.add(
                ScheduleProviderReturnWithTime(
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(practitioner.actor.reference.removePrefix("Practitioner/"), "external")
                    ),
                    providerName = "",
                    time = "",
                    departmentIDs = emptyList()

                )
            )
        }

        return EpicAppointment(
            appointmentDuration = r4Appointment.minutesDuration.toString(),
            appointmentNotes = listOf(r4Appointment.patientInstruction ?: "", r4Appointment.comment ?: ""),
            appointmentStartTime = SimpleDateFormat("hh:mm aa").format(r4Appointment.start),
            appointmentStatus = r4Appointment.status.toCode(),
            contactIDs = listOf(IDType(r4Appointment.id.removePrefix("Appointment/"), "ASN")),
            date = SimpleDateFormat("MM/dd/yyyy").format(r4Appointment.start),
            extraExtensions = listOf(),
            extraItems = listOf(),
            patientIDs = patientIDs ?: listOf(),
            patientName = r4Patient?.name?.find { it.use.toCode() == "usual" }?.nameAsSingleString
                ?: r4Patient?.nameFirstRep?.nameAsSingleString ?: "",
            providers = providers,
            visitTypeIDs = listOf(),
            visitTypeName = r4Appointment.appointmentType.text ?: ""
        )
    }
}
