package com.projectronin.interop.mock.ehr.hl7v2

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HL7TCPServerTest {

    @Test
    fun `start up test`() {
        val server = HL7TCPServer(0)
        assertTrue(server.server.isRunning)
        server.server.stop()
    }
}
