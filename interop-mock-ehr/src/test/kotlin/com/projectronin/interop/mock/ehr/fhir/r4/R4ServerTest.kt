package com.projectronin.interop.mock.ehr.fhir.r4

import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BinaryResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BundleResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CommunicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ConditionResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4DocumentReferenceResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ObservationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerRoleResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Communication
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class R4ServerTest {

    @Test
    fun `add resources correctly test`() {
        val r4Patient = mockk<R4PatientResourceProvider>()
        every { r4Patient.resourceType } returns Patient::class.java
        val r4Condition = mockk<R4ConditionResourceProvider>()
        every { r4Condition.resourceType } returns Condition::class.java
        val r4Appointment = mockk<R4AppointmentResourceProvider>()
        every { r4Appointment.resourceType } returns Appointment::class.java
        val r4Practitioner = mockk<R4PractitionerResourceProvider>()
        every { r4Practitioner.resourceType } returns Practitioner::class.java
        val r4PractitionerRole = mockk<R4PractitionerRoleResourceProvider>()
        every { r4PractitionerRole.resourceType } returns PractitionerRole::class.java
        val r4Location = mockk<R4LocationResourceProvider>()
        every { r4Location.resourceType } returns Location::class.java
        val r4Communication = mockk<R4CommunicationResourceProvider>()
        every { r4Communication.resourceType } returns Communication::class.java
        val r4Bundle = mockk<R4BundleResourceProvider>()
        every { r4Bundle.resourceType } returns Bundle::class.java
        val r4Observation = mockk<R4ObservationResourceProvider>()
        every { r4Observation.resourceType } returns Observation::class.java
        val r4DocumentReference = mockk<R4DocumentReferenceResourceProvider>()
        every { r4DocumentReference.resourceType } returns DocumentReference::class.java
        val r4Binary = mockk<R4BinaryResourceProvider>()
        every { r4Binary.resourceType } returns Binary::class.java
        val server = R4Server(
            r4Patient,
            r4Condition,
            r4Appointment,
            r4Practitioner,
            r4Location,
            r4PractitionerRole,
            r4Communication,
            r4Bundle,
            r4Observation,
            r4DocumentReference,
            r4Binary
        )
        server.init()
        assertTrue(
            server.resourceProviders.containsAll(
                listOf(
                    r4Patient,
                    r4Condition,
                    r4Appointment,
                    r4Practitioner,
                    r4Location,
                    r4PractitionerRole,
                    r4Communication,
                    r4Bundle,
                    r4Observation,
                    r4DocumentReference,
                    r4Binary
                )
            )
        )
    }
}
