package com.projectronin.interop.mock.ehr.hl7v2

import io.mockk.mockk
import jakarta.servlet.ServletConfig
import org.junit.jupiter.api.Test

internal class HL7HTTPServerTest {
    @Test
    fun `init test`() {
        val config = mockk<ServletConfig>()
        HL7HTTPServer(mockk(), mockk(), mockk(), mockk()).init(config) // not much to assert, just code coverage
    }
}
