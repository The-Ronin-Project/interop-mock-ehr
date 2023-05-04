package com.projectronin.interop.mock.ehr.testcontainer

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class InteropMockEHRTestContainerTest {

    private var mockEHR = MockEHRTestcontainer()

    @Test
    fun `start up test`() {
        assertNotNull(mockEHR.getURL())
    }

    @Test
    fun `add resource test`() {
        val createPat = this::class.java.getResource("/r4Patient.json")!!.readText()
        val response = mockEHR.addR4Resource("Patient", createPat, "eJzlzKe3KPzAV5TtkxmNivQ3")
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
