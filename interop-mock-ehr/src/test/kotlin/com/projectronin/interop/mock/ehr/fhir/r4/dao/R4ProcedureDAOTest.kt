package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Procedure
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test edge cases of R4ProcedureDAO.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4ProcedureDAOTest : BaseMySQLTest() {
    private lateinit var dao: R4ProcedureDAO
    private lateinit var collection: Collection

    @BeforeAll
    fun initTest() {
        collection = createCollection(Procedure::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Procedure::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4ProcedureDAO(database, FhirContext.forR4())
    }

    @Test
    fun `nothing passed in`() {
        val output = dao.searchByQuery()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `all params are null or empty`() {
        val output = dao.searchByQuery(emptyList(), null, null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `reference list is empty`() {
        val output = dao.searchByQuery(reference = emptyList())
        assertTrue(output.isEmpty())
    }

    @Test
    fun `fromDate is null`() {
        val output = dao.searchByQuery(fromDate = null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `toDate is null`() {
        val output = dao.searchByQuery(toDate = null)
        assertTrue(output.isEmpty())
    }
}
