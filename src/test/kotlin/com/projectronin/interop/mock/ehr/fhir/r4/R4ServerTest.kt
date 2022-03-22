package com.projectronin.interop.mock.ehr.fhir.r4

import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class R4ServerTest {

    @Test
    fun `add resources correctly test`() {
        val r4Patient = mockk<R4PatientResourceProvider>()
        every { r4Patient.resourceType } returns Patient::class.java
        val r4Appointment = mockk<R4AppointmentResourceProvider>()
        every { r4Appointment.resourceType } returns Appointment::class.java
        val r4Practitioner = mockk<R4PractitionerResourceProvider>()
        every { r4Practitioner.resourceType } returns Practitioner::class.java
        val r4Location = mockk<R4LocationResourceProvider>()
        every { r4Location.resourceType } returns Location::class.java
        val server = R4Server(r4Patient, r4Appointment, r4Practitioner, r4Location)
        server.init()
        assertTrue(server.resourceProviders.containsAll(listOf(r4Patient, r4Appointment, r4Practitioner, r4Location)))
    }
}
