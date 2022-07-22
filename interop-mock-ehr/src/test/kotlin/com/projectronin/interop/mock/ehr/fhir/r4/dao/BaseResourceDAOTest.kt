package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Observation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test edge cases of BaseResourceDAO utility methods. Other cases see R4ObservationResourceTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BaseResourceDAOTest {
    private lateinit var dao: R4ObservationDAO

    @BeforeAll
    fun initTest() {
        val collection = mockk<Collection>()
        val database = mockk<Schema>()
        every { database.createCollection(Observation::class.simpleName, true) } returns collection
        dao = R4ObservationDAO(database)
    }

    @Test
    fun `tokens null`() {
        assertNull(dao.getSearchStringForFHIRTokens(null))
    }

    @Test
    fun `tokens empty pipe - single`() {
        val token = TokenParam()
        token.system = ""
        token.value = ""
        val tokenList = TokenOrListParam()
        tokenList.add(token)
        assertNull(dao.getSearchStringForFHIRTokens(tokenList))
    }

    @Test
    fun `tokens empty pipe - multiple`() {
        val token = TokenParam()
        token.system = ""
        token.value = ""
        val tokenList = TokenOrListParam()
        tokenList.add(token)
        tokenList.add(token)
        assertNull(dao.getSearchStringForFHIRTokens(tokenList))
    }

    @Test
    fun `tokens mixed test cases - multiple`() {
        val expectedString = mutableListOf<String>()
        expectedString.add(" ( ")
        expectedString.add("('i' in category[*].coding[*].system AND 'j' in category[*].coding[*].code) OR ")
        expectedString.add("('x' in category[*].coding[*].code OR 'x' in category[*].text) OR ")
        expectedString.add("('y' in category[*].coding[*].system) OR ")
        expectedString.add("('z' in category[*].coding[*].code OR 'z' in category[*].text)")
        expectedString.add(" ) ")

        val token2 = TokenParam()
        token2.system = "i"
        token2.value = "j"
        val token3 = TokenParam()
        token3.system = ""
        token3.value = "x"
        val token4 = TokenParam()
        token4.system = "y"
        token4.value = ""
        val token5 = TokenParam()
        token5.system = ""
        token5.value = "z"
        val tokenList = TokenOrListParam()
        tokenList.add(token2)
        tokenList.add(token3)
        tokenList.add(token4)
        tokenList.add(token5)

        val searchString = dao.getSearchStringForFHIRTokens(tokenList)
        assertEquals(
            expectedString.joinToString(""),
            searchString
        )
    }

    @Test
    fun `token null`() {
        assertNull(dao.getSearchStringForFHIRToken(null))
    }
}
