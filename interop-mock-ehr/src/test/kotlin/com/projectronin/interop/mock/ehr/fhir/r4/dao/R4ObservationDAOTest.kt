package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Observation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test edge cases of R4ObservationDAO. Other cases see R4ObservationResourceTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4ObservationDAOTest {
    private lateinit var dao: R4ObservationDAO

    @BeforeAll
    fun initTest() {
        val collection = mockk<Collection>()
        val database = mockk<SafeXDev>()
        every { database.createCollection(Observation::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        dao = R4ObservationDAO(database, FhirContext.forR4())
    }

    @Test
    fun `all inputs missing`() {
        val output = dao.searchByQuery()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `all inputs null`() {
        val output = dao.searchByQuery(null, null, null, null, null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `subject null`() {
        val output = dao.searchByQuery(subject = null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `category null`() {
        val output = dao.searchByQuery(category = null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `fromDate null`() {
        val output = dao.searchByQuery(fromDate = null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `toDate null`() {
        val output = dao.searchByQuery(toDate = null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `code null`() {
        val output = dao.searchByQuery(code = null)
        assertTrue(output.isEmpty())
    }
}
