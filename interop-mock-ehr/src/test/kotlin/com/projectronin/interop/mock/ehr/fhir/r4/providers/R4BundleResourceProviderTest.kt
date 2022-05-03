package com.projectronin.interop.mock.ehr.fhir.r4.providers

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class R4BundleResourceProviderTest {

    private var dao = mockk<R4PatientDAO>()
    private lateinit var provider: R4BundleResourceProvider

    @BeforeEach
    fun initTest() {
        every { dao.resourceType } returns Patient::class.java
        provider = R4BundleResourceProvider(listOf(dao))
    }

    @Test
    fun `returns correct resource type`() {
        assertEquals(provider.resourceType, Bundle::class.java)
    }

    @Test
    fun `bundle transaction works`() {
        val bundle = Bundle()
        val entry = Bundle.BundleEntryComponent()
        val request = Bundle.BundleEntryRequestComponent()
        val resource = Patient()
        resource.id = "TESTID"
        request.method = Bundle.HTTPVerb.POST
        entry.request = request
        entry.resource = resource
        bundle.addEntry(entry)
        every { dao.insert(resource) } returns "TESTID"

        assertNotNull(provider.bundleTransaction(bundle))
    }

    @Test
    fun `bundle transaction failure test 1`() {
        val bundle = Bundle()
        val entry = Bundle.BundleEntryComponent()
        val request = Bundle.BundleEntryRequestComponent()
        val resource = Patient()
        resource.id = "TESTID"
        request.method = Bundle.HTTPVerb.DELETE // only allow POST
        entry.request = request
        entry.resource = resource
        bundle.addEntry(entry)

        assertThrows(UnsupportedOperationException::class.java) {
            provider.bundleTransaction(bundle)
        }
    }

    @Test
    fun `bundle transaction failure test 2`() {
        val bundle = Bundle()
        val entry = Bundle.BundleEntryComponent()
        val request = Bundle.BundleEntryRequestComponent()
        val resource = Appointment()
        resource.id = "TESTID"
        request.method = Bundle.HTTPVerb.POST
        entry.request = request
        entry.resource = resource
        bundle.addEntry(entry)

        assertThrows(UnsupportedOperationException::class.java) {
            provider.bundleTransaction(bundle)
        }
    }
}
