package com.projectronin.interop.mock.ehr.cerner

import com.projectronin.interop.ehr.cerner.auth.CernerAuthentication
import com.projectronin.interop.mock.ehr.cerner.dal.CernerDAL
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class CernerServerTest {
    private var dal = mockk<CernerDAL>()
    private var server = CernerServer(dal)

    @Test
    fun `check auth test`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val expected = CernerAuthentication(
            accessToken = "UUID-GENERATED-ID",
            tokenType = "bearer",
            expiresIn = 3600,
            scope = "Patient.read Patient.search",
            refreshToken = null
        )
        val actual = server.getAuthToken()
        assertEquals(expected, actual)
        unmockkStatic(UUID::class)
    }
}
