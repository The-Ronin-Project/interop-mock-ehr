package com.projectronin.interop.mock.ehr.fhir.r4

import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class R4ServerTest {

    @Test
    fun `add resources correctly test`() {
        val r4Patient = mockk<R4PatientResourceProvider>()
        every { r4Patient.resourceType } returns Patient::class.java
        val r4Appointment = mockk<R4AppointmentResourceProvider>()
        every { r4Appointment.resourceType } returns Appointment::class.java
        val server = R4Server(r4Patient, r4Appointment)
        server.init()
        assertTrue(server.resourceProviders.containsAll(listOf(r4Patient, r4Appointment)))
    }
}
