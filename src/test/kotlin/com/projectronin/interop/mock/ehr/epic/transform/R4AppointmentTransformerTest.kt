package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.fhir.r4.ExtensionMeanings
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Date
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as EpicAppointment
import org.hl7.fhir.r4.model.Appointment as R4Appointment

internal class R4AppointmentTransformerTest {

    @Test
    fun `transform with everything test`() {
        val patient = Patient()
        patient.identifier = listOf(Identifier().setValue("PATMRN").setSystem("MRN"))
        patient.addName(HumanName().addGiven("given").setFamily("family").setUse(HumanName.NameUse.USUAL))
        val practitionerParticipant = AppointmentParticipantComponent()
        practitionerParticipant.actor = Reference().setReference("Practitioner/PRACTID#1").setType("Practitioner")
        val partnerReference = Extension()
        partnerReference.url = ExtensionMeanings.PARTNER_DEPARTMENT.uri.toString()
        partnerReference.setValue(Reference("Organization/ORGID#1"))
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
        input.addExtension(partnerReference)

        val expected = EpicAppointment(
            appointmentDuration = "30",
            appointmentNotes = listOf("instruction", "comment"),
            appointmentStartTime = "12:00 AM",
            appointmentStatus = "booked",
            contactIDs = listOf(IDType(id = "APPTID#1", type = "ASN")),
            date = "01/01/2020",
            extraExtensions = listOf(),
            extraItems = listOf(),
            patientIDs = listOf(IDType("PATMRN", "MRN")),
            patientName = "given family",
            providers = listOf(
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(
                        IDType(
                            id = "ORGID#1",
                            type = "FHIR"
                        )
                    ),
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(),
                    providerName = "",
                    time = ""
                ),
                ScheduleProviderReturnWithTime(
                    departmentIDs = listOf(),
                    departmentName = "",
                    duration = "",
                    providerIDs = listOf(
                        IDType(
                            id = "PRACTID#1",
                            type = "FHIR"
                        )
                    ),
                    providerName = "",
                    time = ""
                )
            ),
            visitTypeIDs = listOf(),
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
            contactIDs = listOf(IDType(id = "APPTID#1", type = "ASN")),
            date = "01/01/2020",
            extraExtensions = listOf(),
            extraItems = listOf(),
            patientIDs = listOf(),
            patientName = "",
            providers = listOf(),
            visitTypeIDs = listOf(),
            visitTypeName = ""
        )
        val actual = R4AppointmentTransformer().transformToEpicAppointment(input, patient)
        assertEquals(expected, actual)
    }
}
