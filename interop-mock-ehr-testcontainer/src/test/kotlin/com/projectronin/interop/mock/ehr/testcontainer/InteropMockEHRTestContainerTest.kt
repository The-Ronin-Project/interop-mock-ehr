package com.projectronin.interop.mock.ehr.testcontainer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container

class InteropMockEHRTestContainerTest {
    @Container
    private val env = InteropMockEHRTestContainer()

    @Test
    fun `start up test`() {
        env.start()
        assertNotNull(env.getURL())
    }
}
