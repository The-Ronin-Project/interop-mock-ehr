package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.EpicMedAdminRequest
import com.projectronin.interop.ehr.epic.EpicOrderID
import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SmartDataValue
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.mock.ehr.epic.dal.EpicDAL
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Communication
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.util.Date
import java.util.UUID
import org.hl7.fhir.r4.model.Appointment as R4Appointment

internal class EpicServerTest {

    private var dal = mockk<EpicDAL>()
    private var server = EpicServer(dal)

    @Test
    fun `check auth test`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val expected = EpicAuthentication(
            accessToken = "UUID-GENERATED-ID",
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search"
        )
        val actual = server.getAuthToken()
        assertEquals(expected, actual)
        unmockkStatic(UUID::class)
    }

    @Test
    fun `working patient appointment request test`() {
        val patient = Patient()
        patient.id = "TESTINGID"
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )
        mockkConstructor(Identifier::class)
        val ident = mockk<Identifier>()
        every {
            anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("mockEHRMRNSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns patient

        val appointment1 = R4Appointment()
        appointment1.id = "APPTID1"
        val appointment2 = R4Appointment()
        appointment2.id = "APPTID2"

        mockkConstructor(Reference::class)
        val ref = mockk<Reference>()
        every { anyConstructed<Reference>().setReference("TESTINGID") } returns ref
        every {
            dal.r4AppointmentDAO.searchByQuery(
                references = listOf(ref),
                fromDate = Date(120, 0, 1),
                toDate = Date(121, 0, 1, 23, 59)
            )
        } returns listOf(appointment1, appointment2)

        val epicAppointment1 = mockk<EpicAppointment>()
        val epicAppointment2 = mockk<EpicAppointment>()

        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment1,
                patient
            )
        } returns epicAppointment1
        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment2,
                patient
            )
        } returns epicAppointment2
        val output = server.getAppointmentsByPatient(request)
        val expected = GetAppointmentsResponse(
            appointments = listOf(epicAppointment1, epicAppointment2),
            error = null
        )
        assertEquals(expected, output)
        unmockkAll()
    }

    @Test
    fun `working provider appointment request test`() {
        val patient = Patient()
        patient.id = "TESTINGPATID"
        every {
            dal.r4PatientDAO.findById(
                "TESTINGPATID"
            )
        } returns patient

        val provider = Practitioner()
        provider.id = "TESTINGPRACTID"

        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )

        mockkConstructor(Identifier::class)
        mockkConstructor(CodeableConcept::class)
        val ident = mockk<Identifier>()
        val mockCodeableConcept = mockk<CodeableConcept>()
        every { anyConstructed<CodeableConcept>().setText("External") } returns mockCodeableConcept
        every { anyConstructed<Identifier>().setValue("PRACT#1").setType(mockCodeableConcept) } returns ident
        every {
            dal.r4PractitionerDAO.searchByIdentifier(
                ident
            )
        } returns provider

        val appointment1 = R4Appointment()
        appointment1.id = "APPTID1"
        val patientParticipant = org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent()
        patientParticipant.actor = Reference().setReference("Patient/TESTINGPATID").setType("Patient")
        appointment1.participant.add(patientParticipant)
        val appointment2 = R4Appointment()
        appointment2.id = "APPTID2"

        mockkConstructor(Reference::class)
        val ref = mockk<Reference>()
        every { anyConstructed<Reference>().setReference("TESTINGPRACTID") } returns ref

        every {
            dal.r4AppointmentDAO.searchByQuery(
                references = listOf(ref),
                fromDate = Date(120, 0, 1),
                toDate = Date(121, 0, 1, 23, 59)
            )
        } returns listOf(appointment1, appointment2)

        val epicAppointment1 = mockk<EpicAppointment>()
        val epicAppointment2 = mockk<EpicAppointment>()

        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment1,
                patient
            )
        } returns epicAppointment1

        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment2,
                null
            )
        } returns epicAppointment2

        val output = server.getAppointmentsByPractitioner(request)
        val expected = GetAppointmentsResponse(
            appointments = listOf(epicAppointment1, epicAppointment2),
            error = null
        )
        assertEquals(expected, output)
        unmockkAll()
    }

    @Test
    fun `working department appointment request test`() {
        val patient = Patient()
        patient.id = "TESTINGPATID"
        every {
            dal.r4PatientDAO.findById(
                "TESTINGPATID"
            )
        } returns patient

        val location = Location()
        location.id = "TESTINGLOCID"

        val request = GetProviderAppointmentRequest(
            departments = listOf(
                IDType("DEPT#1", "Internal")
            ),
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )

        mockkConstructor(Identifier::class)
        mockkConstructor(CodeableConcept::class)
        val ident = mockk<Identifier>()
        val mockCodeableConcept = mockk<CodeableConcept>()
        every { anyConstructed<CodeableConcept>().setText("External") } returns mockCodeableConcept
        every {
            anyConstructed<Identifier>().setValue("DEPT#1").setSystem("mockEHRDepartmentInternalSystem")
        } returns ident
        every {
            dal.r4LocationDAO.searchByIdentifier(
                ident
            )
        } returns location

        val appointment1 = R4Appointment()
        appointment1.id = "APPTID1"
        val patientParticipant = AppointmentParticipantComponent()
        patientParticipant.actor = Reference().setReference("Patient/TESTINGPATID").setType("Patient")
        appointment1.participant.add(patientParticipant)
        val appointment2 = R4Appointment()
        appointment2.id = "APPTID2"

        mockkConstructor(Reference::class)
        val ref = mockk<Reference>()
        every { anyConstructed<Reference>().setReference("TESTINGLOCID") } returns ref

        every {
            dal.r4AppointmentDAO.searchByQuery(
                references = listOf(ref),
                fromDate = Date(120, 0, 1),
                toDate = Date(121, 0, 1, 23, 59)
            )
        } returns listOf(appointment1, appointment2)

        val epicAppointment1 = mockk<EpicAppointment>()
        val epicAppointment2 = mockk<EpicAppointment>()

        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment1,
                patient
            )
        } returns epicAppointment1

        every {
            dal.r4AppointmentTransformer.transformToEpicAppointment(
                appointment2,
                null
            )
        } returns epicAppointment2

        val output = server.getAppointmentsByPractitioner(request)
        val expected = GetAppointmentsResponse(
            appointments = listOf(epicAppointment1, epicAppointment2),
            error = null
        )
        assertEquals(expected, output)
        unmockkAll()
    }

    @Test
    fun `no patient found test`() {
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )
        mockkConstructor(Identifier::class)
        mockkConstructor(CodeableConcept::class)
        val ident = mockk<Identifier>()
        every {
            anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("mockEHRMRNSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns null
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPatient(request) }
        assertEquals("NO-PATIENT-FOUND", output.reason)
        unmockkAll()
    }

    @Test
    fun `bad date test 1`() {
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "lalala",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPatient(request) }
        assertEquals("INVALID-START-DATE", output.reason)
    }

    @Test
    fun `bad date test 2`() {
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "01/01/2020",
            endDate = "lalala",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPatient(request) }
        assertEquals("INVALID-END-DATE", output.reason)
    }

    @Test
    fun `bad date test 3`() {
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "01/01/2020",
            endDate = "01/01/2019",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPatient(request) }
        assertEquals("END-DATE-BEFORE-START-DATE", output.reason)
    }

    @Test
    fun `no end date test`() {
        val request = GetPatientAppointmentsRequest(
            patientId = "TESTINGMRN",
            patientIdType = "",
            startDate = "01/01/2020",
            endDate = null,
            userID = null,
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPatient(request) }
        assertEquals("NO-USER-FOUND", output.reason)
    }

    @Test
    fun `provider bad date test 1`() {
        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "lalala",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("INVALID-START-DATE", output.reason)
    }

    @Test
    fun `provider bad date test 2`() {
        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "01/01/2020",
            endDate = "lalala",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("INVALID-END-DATE", output.reason)
    }

    @Test
    fun `provider bad date test 3`() {
        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "01/01/2020",
            endDate = "01/01/2019",
            userID = "12345",
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("END-DATE-BEFORE-START-DATE", output.reason)
    }

    @Test
    fun `provider no end date test`() {
        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "01/01/2020",
            endDate = null,
            userID = null,
            userIDType = "Internal"
        )
        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("NO-USER-FOUND", output.reason)
    }

    @Test
    fun `no practitioners found test 1`() {
        val request = GetProviderAppointmentRequest(
            providers = listOf(
                ScheduleProvider(
                    "PRACT#1",
                    "External"
                )
            ),
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )
        mockkConstructor(Identifier::class)
        mockkConstructor(CodeableConcept::class)
        val ident = mockk<Identifier>()
        val mockCodeableConcept = mockk<CodeableConcept>()
        every { anyConstructed<CodeableConcept>().setText("External") } returns mockCodeableConcept
        every { anyConstructed<Identifier>().setValue("PRACT#1").setType(mockCodeableConcept) } returns ident
        every {
            dal.r4PractitionerDAO.searchByIdentifier(
                ident
            )
        } returns null

        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("NO-PROVIDER-FOUND", output.reason)
    }

    @Test
    fun `no practitioners found test 2`() {
        val request = GetProviderAppointmentRequest(
            providers = null,
            startDate = "01/01/2020",
            endDate = "01/01/2021",
            userID = "12345",
            userIDType = "Internal"
        )

        val output = assertThrows<ResponseStatusException> { server.getAppointmentsByPractitioner(request) }
        assertEquals("NO-PROVIDER-FOUND", output.reason)
    }

    @Test
    fun `onboard flag test - error`() {
        val request = SetSmartDataValuesRequest(
            idType = "MRN",
            id = "12345",
            userID = "1",
            userIDType = "External",
            source = "Ronin",
            contextName = "PATIENT",
            smartDataValues = listOf(
                SmartDataValue(
                    comments = listOf("Comment"),
                    values = "Value",
                    smartDataIDType = "SDI",
                    smartDataID = "SDEID"
                )
            )
        )
        every { dal.r4PatientDAO.searchByIdentifier(any()) } returns null
        val output = assertThrows<ResponseStatusException> { server.addOnboardFlag(request) }
        assertTrue(output.reason!!.contains("NO-ENTITY-FOUND"))
    }

    @Test
    fun `onboard flag test - success`() {
        val request = SetSmartDataValuesRequest(
            idType = "MRN",
            id = "12345",
            userID = "1",
            userIDType = "External",
            source = "Ronin",
            contextName = "PATIENT",
            smartDataValues = listOf(
                SmartDataValue(
                    comments = listOf("Comment"),
                    values = "Value",
                    smartDataIDType = "SDI",
                    smartDataID = "SDEID"
                )
            )
        )
        every { dal.r4FlagDAO.searchByQuery(any()) } returns listOf()
        every { dal.r4FlagDAO.insert(any()) } returns ""
        every { dal.r4PatientDAO.searchByIdentifier(any()) } returns mockk {
            every { id } returns "12345"
        }
        val output = server.addOnboardFlag(request)
        assertTrue(output.success)
    }

    @Test
    fun `working communication test`() {
        val request = SendMessageRequest(
            messageText = listOf("Message Text", "Line 2"),
            patientID = "TESTINGMRN",
            recipients = listOf(
                SendMessageRecipient("first", false, "External"),
                SendMessageRecipient("second", true, "External")
            ),
            senderID = "Sender#1",
            messageType = "messageType",
            senderIDType = "SendType#1",
            patientIDType = "MRN",
            contactID = "Con#1",
            contactIDType = "ConType#1",
            messagePriority = "just incoherent gibberish"
        )
        val communication = mockk<Communication>()
        every {
            dal.r4CommunicationTransformer.transformFromSendMessage(request)
        } returns communication
        every {
            dal.r4CommunicationDAO.insert(communication)
        } returns "NEW FHIR ID"
        mockkConstructor(Identifier::class)
        val ident = mockk<Identifier>()
        val patient = Patient()
        patient.id = "TESTINGID"
        every {
            anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("mockEHRMRNSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns patient
        val output = server.createCommunication(request)
        assertEquals(1, output.idTypes.size)
        assertEquals("NEW FHIR ID", output.idTypes.first().id)
        assertEquals("FHIR ID", output.idTypes.first().type)

        every {
            anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("MRN")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns null
        assertThrows<ResponseStatusException> { server.createCommunication(request) }
    }

    @Test
    fun `working med admin test`() {
        val request = EpicMedAdminRequest(
            patientID = "TESTINGINTERNAL",
            patientIDType = "Internal",
            contactID = "Con#1",
            contactIDType = "ConType#1",
            orderIDs = listOf(
                EpicOrderID(
                    "MedAdmin#1",
                    "External"
                )
            )
        )

        mockkConstructor(Identifier::class)
        val ident = mockk<Identifier>()
        val patient = Patient()
        patient.id = "TESTINGID"
        every {
            anyConstructed<Identifier>().setValue("TESTINGINTERNAL").setSystem("mockPatientInternalSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns patient
        every { dal.r4MedicationRequestDAO.searchByQuery(any(), any(), any(), any()) } returns listOf(
            mockk {
                every { id } returns "MedRequest#1"
            }
        )
        every { dal.r4MedAdminDAO.searchByRequest("MedRequest#1") } returns listOf(
            mockk {
                every { effectiveDateTimeType.valueAsString } returns "dateTime"
                every { medicationCodeableConcept.text } returns "MedicationName"
                every { dosage.dose } returns mockk {
                    every { unit } returns "unit"
                    every { value.toPlainString() } returns "1.0"
                }
                every { status } returns MedicationAdministration.MedicationAdministrationStatus.COMPLETED
            }
        )
        val output = server.getMedicationAdministration(request)
        assertEquals(output.orders.size, 1)
    }

    @Test
    fun `med admin test fails - no patient`() {
        val request = EpicMedAdminRequest(
            patientID = "TESTINGINTERNAL",
            patientIDType = "Internal",
            contactID = "Con#1",
            contactIDType = "ConType#1",
            orderIDs = listOf(
                EpicOrderID(
                    "MedAdmin#1",
                    "External"
                )
            )
        )

        mockkConstructor(Identifier::class)
        val ident = mockk<Identifier>()
        val patient = Patient()
        patient.id = "TESTINGID"
        every {
            anyConstructed<Identifier>().setValue("TESTINGINTERNAL").setSystem("mockPatientInternalSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns null
        assertThrows<ResponseStatusException> { server.getMedicationAdministration(request) }
    }

    @Test
    fun `med admin test fails - no med request`() {
        val request = EpicMedAdminRequest(
            patientID = "TESTINGINTERNAL",
            patientIDType = "Internal",
            contactID = "Con#1",
            contactIDType = "ConType#1",
            orderIDs = listOf(
                EpicOrderID(
                    "MedAdmin#1",
                    "External"
                )
            )
        )

        mockkConstructor(Identifier::class)
        val ident = mockk<Identifier>()
        val patient = Patient()
        patient.id = "TESTINGID"
        every {
            anyConstructed<Identifier>().setValue("TESTINGINTERNAL").setSystem("mockPatientInternalSystem")
        } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns patient
        every { dal.r4MedicationRequestDAO.searchByQuery(any(), any(), any(), any()) } returns listOf()
        assertThrows<ResponseStatusException> { server.getMedicationAdministration(request) }
    }
}
