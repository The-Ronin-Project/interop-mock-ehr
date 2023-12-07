package com.projectronin.interop.mock.ehr.stu3

import ca.uhn.fhir.context.FhirContext
import com.projectronin.interop.mock.ehr.fhir.stu3.STU3Server
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3MedicationStatementProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.dstu3.model.Appointment
import org.hl7.fhir.dstu3.model.MedicationStatement
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class STU3ServerTest {
    @Test
    fun `add resources correctly test`() {
        val ctx = FhirContext.forDstu3()
        val stu3Appointment = mockk<STU3AppointmentResourceProvider>()
        val stu3MedicationStatement = mockk<STU3MedicationStatementProvider>()
        every { stu3Appointment.resourceType } returns Appointment::class.java
        every { stu3MedicationStatement.resourceType } returns MedicationStatement::class.java
        val server = STU3Server(ctx, stu3Appointment, stu3MedicationStatement)
        server.init()
        assertTrue(
            server.resourceProviders.containsAll(
                listOf(
                    stu3Appointment,
                    stu3MedicationStatement,
                ),
            ),
        )
    }
}
