package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.DiagnosticReport
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4DiagnosticReportDAOTest : BaseMySQLTest() {
    private lateinit var dao: R4DiagnosticReportDAO
    private lateinit var collection: Collection

    @BeforeAll
    fun beforeTest() {
        collection = createCollection(DiagnosticReport::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(DiagnosticReport::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4DiagnosticReportDAO(database, FhirContext.forR4())
    }

    @Test
    fun `works without any input`() {
        val output = dao.searchByQuery("")
        Assertions.assertTrue(output.isEmpty())
    }

    @Test
    fun `works with all null input`() {
        val output = dao.searchByQuery("", null, null)
        Assertions.assertTrue(output.isEmpty())
    }

    @Test
    fun `search works with null string input`() {
        val output = dao.searchByQuery("")
        Assertions.assertTrue(output.isEmpty())
    }

    @Test
    fun `fromDate null`() {
        val output = dao.searchByQuery("", fromDate = null)
        Assertions.assertTrue(output.isEmpty())
    }

    @Test
    fun `toDate null`() {
        val output = dao.searchByQuery("", toDate = null)
        Assertions.assertTrue(output.isEmpty())
    }
}
