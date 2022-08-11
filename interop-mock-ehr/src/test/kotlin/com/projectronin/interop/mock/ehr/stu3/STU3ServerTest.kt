package com.projectronin.interop.mock.ehr.stu3

import com.projectronin.interop.mock.ehr.fhir.stu3.STU3Server
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.dstu3.model.Appointment
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class STU3ServerTest {

    @Test
    fun `add resources correctly test`() {
        val stu3Appointment = mockk<STU3AppointmentResourceProvider>()
        every { stu3Appointment.resourceType } returns Appointment::class.java
        val server = STU3Server(stu3Appointment)
        server.init()
        assertTrue(
            server.resourceProviders.containsAll(
                listOf(
                    stu3Appointment
                )
            )
        )
    }
}
