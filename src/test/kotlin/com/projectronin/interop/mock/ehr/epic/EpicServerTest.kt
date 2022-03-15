package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import com.projectronin.interop.mock.ehr.epic.dal.EpicDAL
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as EpicAppointment
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
        every { anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("MRN") } returns ident
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
                toDate = Date(121, 0, 1)
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
            error = "No error good job"
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
        val ident = mockk<Identifier>()
        every { anyConstructed<Identifier>().setValue("TESTINGMRN").setSystem("MRN") } returns ident
        every {
            dal.r4PatientDAO.searchByIdentifier(
                ident
            )
        } returns null
        val output = server.getAppointmentsByPatient(request)
        val expected = GetAppointmentsResponse(
            appointments = listOf(),
            error = "No patient found."
        )
        assertEquals(expected, output)
    }
}
