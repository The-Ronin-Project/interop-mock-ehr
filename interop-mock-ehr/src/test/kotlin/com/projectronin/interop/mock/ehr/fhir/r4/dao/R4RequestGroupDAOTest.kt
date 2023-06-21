package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.RequestGroup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4RequestGroupDAOTest : BaseMySQLTest() {
    private lateinit var dao: R4RequestGroupDAO
    private lateinit var collection: Collection

    @BeforeAll
    fun initTest() {
        collection = createCollection(RequestGroup::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every {
            database.createCollection(RequestGroup::class.java)
        } returns SafeXDev.SafeCollection("resource", collection)
        dao = R4RequestGroupDAO(database, FhirContext.forR4())
    }

    @Test
    fun `works without any input`() {
        val output = dao.searchByQuery()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `works with all null input`() {
        val output = dao.searchByQuery(null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search works with empty list input`() {
        val output = dao.searchByQuery(StringOrListParam())
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search works with empty param input`() {
        val searchParams = StringOrListParam()
        searchParams.add(StringParam())
        val output = dao.searchByQuery(searchParams)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search works with null list`() {
        val searchParam = StringOrListParam()
        searchParam.add(null)
        val output = dao.searchByQuery(searchParam)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search work with null value`() {
        val searchParams = StringOrListParam()
        searchParams.add(StringParam(null))
        val output = dao.searchByQuery(searchParams)
        assertTrue(output.isEmpty())
    }
}
