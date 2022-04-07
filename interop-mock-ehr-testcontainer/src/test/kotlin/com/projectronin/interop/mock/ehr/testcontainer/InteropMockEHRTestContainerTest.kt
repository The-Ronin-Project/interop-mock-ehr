package com.projectronin.interop.mock.ehr.testcontainer

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class InteropMockEHRTestContainerTest : BaseMockEHRTest() {

    @Test
    fun `start up test`() {
        assertNotNull(this.getURL())
    }

    @Test
    fun `add resource test`() {
        val createPat = this::class.java.getResource("/r4Patient.json")!!.readText()
        val response = addR4Resource("Patient", createPat, "eJzlzKe3KPzAV5TtkxmNivQ3")
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
