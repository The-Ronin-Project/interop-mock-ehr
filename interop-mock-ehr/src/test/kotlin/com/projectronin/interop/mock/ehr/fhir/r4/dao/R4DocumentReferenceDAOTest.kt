package com.projectronin.interop.mock.ehr.fhir.r4.dao

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.DbDoc
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

/**
 * Test edge cases of R4DocumentReferenceDAO. Other cases see R4DocumentReferenceResourceTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class R4DocumentReferenceDAOTest {
    private lateinit var dao: R4DocumentReferenceDAO
    private lateinit var collection: Collection

    @BeforeAll
    fun initTest() {
        collection = mockk()
        val database = mockk<SafeXDev>()
        every { database.createCollection(DocumentReference::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4DocumentReferenceDAO(database, FhirContext.forR4())
    }

    @Test
    fun `all inputs missing`() {
        val output = dao.searchByQuery()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `all inputs null`() {
        val output = dao.searchByQuery(null, null)
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
    fun `identifier search returns null when none found`() {
        every { collection.find(any()) } returns
            mockk {
                every { execute() } returns
                    mockk {
                        every { fetchAll() } returns emptyList()
                    }
            }
        val identifier = Identifier()
        identifier.value = "value"
        val output = dao.searchByIdentifier(identifier)
        assertNull(output)
    }

    @Test
    fun `identifier search returns null when multiple found`() {
        every { collection.find(any()) } returns
            mockk {
                every { execute() } returns
                    mockk {
                        every { fetchAll() } returns listOf(mockk(), mockk())
                    }
            }
        val identifier = Identifier()
        identifier.value = "value"
        val output = dao.searchByIdentifier(identifier)
        assertNull(output)
    }

    @Test
    fun `identifier search finds values`() {
        val document = DocumentReference()
        document.id = UUID.randomUUID().toString()
        val documentString = FhirContext.forR4().newJsonParser().encodeResourceToString(document)
        val mockDocDb = mockk<DbDoc>()
        every { mockDocDb.toString() } returns documentString
        every { collection.find(any()) } returns
            mockk {
                every { execute() } returns
                    mockk {
                        every { fetchAll() } returns listOf(mockDocDb)
                    }
            }
        val identifier = Identifier()
        identifier.value = "value"
        val output = dao.searchByIdentifier(identifier)
        assertEquals(document.id, output?.id?.substringAfterLast("/"))
    }
}
