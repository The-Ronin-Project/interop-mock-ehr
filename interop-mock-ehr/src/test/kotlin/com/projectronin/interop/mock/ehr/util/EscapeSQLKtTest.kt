package com.projectronin.interop.mock.ehr.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EscapeSQLKtTest {
    @Test
    fun `escapes single quote`() {
        assertEquals("test''", "test'".escapeSQL())
    }

    @Test
    fun `does not mess up string with no escape characters`() {
        assertEquals("test", "test".escapeSQL())
    }
}
