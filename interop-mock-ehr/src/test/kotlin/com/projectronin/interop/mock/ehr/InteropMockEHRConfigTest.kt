package com.projectronin.interop.mock.ehr

import ca.uhn.fhir.context.FhirVersionEnum
import com.projectronin.interop.mock.ehr.xdevapi.XDevConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class InteropMockEHRConfigTest {
    @Test
    fun `return correct URL`() {
        val xdevConfig = InteropMockEHRConfig().xdevConfig("host", "9090", "name", "user", "pass")
        assertEquals(XDevConfig("host", "9090", "name", "user", "pass"), xdevConfig)
    }

    @Test
    fun `code coverage`() {
        val filter = InteropMockEHRConfig().logFilter()
        assertNotNull(filter)
    }

    @Test
    fun `return r4`() {
        assertEquals(InteropMockEHRConfig().r4Context().version, FhirVersionEnum.R4.versionImplementation)
    }

    @Test
    fun `return dstu3`() {
        assertEquals(InteropMockEHRConfig().dstu3Context().version, FhirVersionEnum.DSTU3.versionImplementation)
    }
}
