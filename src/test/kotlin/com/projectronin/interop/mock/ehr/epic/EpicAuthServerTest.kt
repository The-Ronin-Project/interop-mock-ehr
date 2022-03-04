package com.projectronin.interop.mock.ehr.epic

import com.projectronin.interop.ehr.epic.auth.EpicAuthentication
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class EpicAuthServerTest {

    @Test
    fun `check auth test`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val expected = EpicAuthentication(
            accessToken = "UUID-GENERATED-ID",
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search"
        )
        val actual = EpicServer().getAuthToken()
        assertEquals(expected, actual)
        unmockkStatic(UUID::class)
    }
}
