package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.SessionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertEquals
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
}
