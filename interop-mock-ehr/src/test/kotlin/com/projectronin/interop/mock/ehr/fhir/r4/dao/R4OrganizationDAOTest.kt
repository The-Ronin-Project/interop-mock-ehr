package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Organization
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test edge cases of R4OrganizationDAO. Other cases see R4OrganizationResourceTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4OrganizationDAOTest : BaseMySQLTest() {
    private lateinit var dao: R4OrganizationDAO
    private lateinit var collection: Collection

    @BeforeAll
    fun initTest() {
        collection = createCollection(Organization::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Organization::class.simpleName, true) } returns collection
        dao = R4OrganizationDAO(database, FhirContext.forR4())
    }

    @Test
    fun `all inputs missing`() {
        val output = dao.searchByQuery()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `all inputs null`() {
        val output = dao.searchByQuery(null)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search one or more ids - empty list input`() {
        val output = dao.searchByQuery(StringOrListParam())
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search one or more ids - empty param input`() {
        val params = StringOrListParam()
        params.add(StringParam())
        val output = dao.searchByQuery(params)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search one or more ids - null param input`() {
        val params = StringOrListParam()
        params.add(null)
        val output = dao.searchByQuery(params)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search one or more ids - null param value input`() {
        val params = StringOrListParam()
        params.add(StringParam(null))
        val output = dao.searchByQuery(params)
        assertTrue(output.isEmpty())
    }
}
