package com.projectronin.interop.mock.ehr.testcontainer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class InteropMockEHRTestContainerTest : BaseMockEHRTest() {

    @Test
    fun `start up test`() {
        assertNotNull(this.getURL())
    }
}
