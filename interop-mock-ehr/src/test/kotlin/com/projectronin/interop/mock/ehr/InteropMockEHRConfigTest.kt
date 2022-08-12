package com.projectronin.interop.mock.ehr

import ca.uhn.fhir.context.FhirVersionEnum
import com.mysql.cj.xdevapi.SessionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class InteropMockEHRConfigTest {

    @Test
    fun `return correct URL`() {
        val slot = slot<String>()
        mockkConstructor(SessionFactory::class)
        every { anyConstructed<SessionFactory>().getSession(capture(slot)).defaultSchema } returns mockk()
        InteropMockEHRConfig().database("host", "9090", "name", "user", "pass")
        assertEquals("mysqlx://host:9090/name?user=user&password=pass", slot.captured)
        unmockkAll()
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
