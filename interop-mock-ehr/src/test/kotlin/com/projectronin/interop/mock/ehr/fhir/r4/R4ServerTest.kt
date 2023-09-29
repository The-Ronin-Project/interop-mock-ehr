package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.server.RequestDetails
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BinaryResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BundleResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CarePlanResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CareTeamResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CommunicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ConditionResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4DocumentReferenceResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4EncounterResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4FlagResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationAdministrationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationRequestResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationStatementResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ObservationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4OrganizationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerRoleResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4RequestGroupResourceProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Communication
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Medication
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.RequestGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.servlet.http.HttpServletResponse

internal class R4ServerTest {

    @Test
    fun `add resources correctly test`() {
        val ctx = FhirContext.forR4()
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
        val r4Organization = mockk<R4OrganizationResourceProvider>()
        every { r4Organization.resourceType } returns Organization::class.java
        val r4CareTeam = mockk<R4CareTeamResourceProvider>()
        every { r4CareTeam.resourceType } returns CareTeam::class.java
        val r4CarePlan = mockk<R4CarePlanResourceProvider>()
        every { r4CarePlan.resourceType } returns CarePlan::class.java
        val r4Medication = mockk<R4MedicationResourceProvider>()
        every { r4Medication.resourceType } returns Medication::class.java
        val r4MedicationStatement = mockk<R4MedicationStatementResourceProvider>()
        every { r4MedicationStatement.resourceType } returns MedicationStatement::class.java
        val r4MedicationRequest = mockk<R4MedicationRequestResourceProvider>()
        every { r4MedicationRequest.resourceType } returns MedicationRequest::class.java
        val r4Encounter = mockk<R4EncounterResourceProvider>()
        every { r4Encounter.resourceType } returns Encounter::class.java
        val r4RequestGroup = mockk<R4RequestGroupResourceProvider>()
        every { r4RequestGroup.resourceType } returns RequestGroup::class.java
        val r4Flag = mockk<R4FlagResourceProvider>()
        every { r4Flag.resourceType } returns Flag::class.java
        val r4MedAdmin = mockk<R4MedicationAdministrationResourceProvider>()
        every { r4MedAdmin.resourceType } returns MedicationAdministration::class.java
        val server = R4Server(
            ctx,
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
            r4Binary,
            r4Organization,
            r4CareTeam,
            r4CarePlan,
            r4Medication,
            r4MedicationStatement,
            r4MedicationRequest,
            r4Encounter,
            r4RequestGroup,
            r4Flag,
            r4MedAdmin
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
                    r4Binary,
                    r4Organization,
                    r4CareTeam,
                    r4CarePlan,
                    r4Medication,
                    r4MedicationStatement,
                    r4MedicationRequest,
                    r4Encounter,
                    r4RequestGroup,
                    r4Flag,
                    r4MedAdmin
                )
            )
        )
    }

    @Test
    fun `filterTest - short circuit null`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns null
        }
        val response = mockk<HttpServletResponse>()
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, true)
    }

    @Test
    fun `filterTest - short circuit empty`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns ""
        }
        val response = mockk<HttpServletResponse>()
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, true)
    }

    @Test
    fun `filterTest - default server`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns "Patient"
            every { completeUrl } returns "localhost/"
        }
        val response = mockk<HttpServletResponse>()
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, true)
    }

    @Test
    fun `filterTest - epic server`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns "Patient"
            every { completeUrl } returns "localhost/epic/"
            every { tenantId = "epic" } just runs
        }
        val response = mockk<HttpServletResponse>()
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, true)
    }

    @Test
    fun `filterTest - cerner server`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns "Patient"
            every { completeUrl } returns "localhost/cerner/"
            every { tenantId = "cerner" } just runs
        }
        val response = mockk<HttpServletResponse>()
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, true)
    }

    @Test
    fun `filterTest - unsupported failure`() {
        val filter = RoninVendorFilter()
        val request = mockk<RequestDetails> {
            every { resourceName } returns "PractitionerRole"
            every { completeUrl } returns "localhost/cerner/"
            every { tenantId = "cerner" } just runs
        }
        val response = mockk<HttpServletResponse> {
            every { sendError(any(), any()) } just runs
        }
        val ret = filter.filterVendor(request, response)
        assertEquals(ret, false)
    }
}
