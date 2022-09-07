package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Date
import org.hl7.fhir.r4.model.Appointment as R4Appointment

internal class R4AppointmentTransformerTest {

    @Test
    fun `transform with everything test`() {
        val patient = Patient()
        patient.identifier = listOf(Identifier().setValue("PATMRN").setSystem("MRN"))
        patient.addName(HumanName().addGiven("given").setFamily("family").setUse(HumanName.NameUse.USUAL))
        val practitionerParticipant = AppointmentParticipantComponent()
        practitionerParticipant.actor = Reference().setReference("Practitioner/PRACTID#1").setType("Practitioner")
        val input = R4Appointment()
        input.appointmentType = CodeableConcept().setText("type")
        input.minutesDuration = 30
        input.patientInstruction = "instruction"
        input.comment = "comment"
        input.start = Date(120, 0, 1)
        input.status = org.hl7.fhir.r4.model.Appointment.AppointmentStatus.BOOKED
        input.id = "Appointment/APPTID#1"
        input.participant = listOf(
            practitionerParticipant
        )

        val expected = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("instruction", "comment"),
            appointmentStartTime = "12:00 AM",
            appointmentStatus = "booked",
            contactIDs = listOf(IDType(id = "APPTID#1", type = "CSN")),
            date = "01/01/2020",
            patientIDs = listOf(IDType("PATMRN", "MRN")),
            patientName = "given family",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = emptyList(),
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(
                            id = "PRACTID#1",
                            type = "external"
                        )
                    ),
                    providerName = "",
                    time = ""
                )
            ),
            visitTypeName = "type"
        )
        val actual = R4AppointmentTransformer().transformToEpicAppointment(input, patient)
        assertEquals(expected, actual)
    }

    @Test
    fun `transform with some null test`() {
        val patient = Patient()
        val input = R4Appointment()
        input.minutesDuration = 30
        input.start = Date(120, 0, 1)
        input.status = org.hl7.fhir.r4.model.Appointment.AppointmentStatus.BOOKED
        input.id = "Appointment/APPTID#1"
        input.participant = listOf()

        val expected = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("", ""),
            appointmentStartTime = "12:00 AM",
            appointmentStatus = "booked",
            contactIDs = listOf(IDType(id = "APPTID#1", type = "CSN")),
            date = "01/01/2020",
            patientIDs = listOf(),
            patientName = "",
            providers = listOf(),
            visitTypeName = ""
        )
        val actual = R4AppointmentTransformer().transformToEpicAppointment(input, patient)
        assertEquals(expected, actual)
    }

    @Test
    fun `correctly translate the special systems`() {
        val patient = Patient()
        patient.identifier =
            listOf(
                Identifier().setValue("PATMRN").setSystem("mockEHRMRNSystem"),
                Identifier().setValue("   Z123").setSystem("mockPatientInternalSystem")
            )
        patient.addName(HumanName().addGiven("given").setFamily("family").setUse(HumanName.NameUse.USUAL))
        val practitionerParticipant = AppointmentParticipantComponent()
        practitionerParticipant.actor = Reference().setReference("Practitioner/PRACTID#1").setType("Practitioner")
        val input = R4Appointment()
        input.appointmentType = CodeableConcept().setText("type")
        input.minutesDuration = 30
        input.patientInstruction = "instruction"
        input.comment = "comment"
        input.start = Date(120, 0, 1)
        input.status = org.hl7.fhir.r4.model.Appointment.AppointmentStatus.BOOKED
        input.id = "Appointment/APPTID#1"
        input.participant = listOf(
            practitionerParticipant
        )

        val expected = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("instruction", "comment"),
            appointmentStartTime = "12:00 AM",
            appointmentStatus = "booked",
            contactIDs = listOf(IDType(id = "APPTID#1", type = "CSN")),
            date = "01/01/2020",
            patientIDs = listOf(
                IDType("PATMRN", "MRN"),
                IDType("   Z123", "Internal")
            ),
            patientName = "given family",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = emptyList(),
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(
                            id = "PRACTID#1",
                            type = "external"
                        )
                    ),
                    providerName = "",
                    time = ""
                )
            ),
            visitTypeName = "type"
        )
        val actual = R4AppointmentTransformer().transformToEpicAppointment(input, patient)
        assertEquals(expected.patientIDs, actual.patientIDs)
    }
}
