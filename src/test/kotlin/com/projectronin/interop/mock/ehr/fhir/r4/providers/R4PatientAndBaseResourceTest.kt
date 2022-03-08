package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Date
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4PatientAndBaseResourceTest {
    private lateinit var collection: Collection
    private lateinit var provider: R4PatientResourceProvider

    @BeforeAll
    fun initTest() {
        val databaseMock = mockk<Schema>()
        val actualFakeDatabase = MySQLContainer<Nothing>("mysql:8.0.28-oracle").apply {
            withExposedPorts(3306, 33060)
        }
        actualFakeDatabase.start()
        val address = actualFakeDatabase.host
        val port = actualFakeDatabase.getMappedPort(33060)
        val databaseSession =
            SessionFactory().getSession("mysqlx://$address:$port/test?user=test&password=test").defaultSchema
        collection = databaseSession.createCollection("test")
        mockkConstructor(SessionFactory::class)

        every {
            anyConstructed<SessionFactory>()
                .getSession("mysqlx://localhost:3306/mock_ehr_db?user=springuser&password=ThePassword")
                .defaultSchema
        } returns databaseMock

        every { databaseMock.createCollection(Patient::class.simpleName, true) } returns collection
        provider = R4PatientResourceProvider()
    }

    @AfterAll
    fun finishTest() {
        unmockkAll()
    }

    @Test
    fun `insert test with id`() {
        val testPat = Patient()
        testPat.id = "TESTINGID"
        testPat.birthDate = Date(87, 0, 15)

        val output = provider.create(testPat)
        assertEquals(output.id, IdType("TESTINGID"))
    }

    @Test
    fun `insert test without id`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val output = provider.create(testPat)
        assertEquals(output.id, IdType("UUID-GENERATED-ID"))
        unmockkStatic(UUID::class)
    }

    @Test
    fun `update test with id`() {

        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        testPat.active = true
        val output = provider.update(IdType("TESTINGID3"), testPat)
        assertTrue(output.created)
        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID3").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertTrue(outputPat.active)
    }

    @Test
    fun `update test without id`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        testPat.active = true
        val output = provider.updateNoId(testPat)
        assertTrue(output.created)
        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID4").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertTrue(outputPat.active)
    }

    @Test
    fun `update test not found so create new`() {
        val testPat = Patient()

        testPat.id = "TESTINGIDNEW"
        testPat.birthDate = Date(87, 0, 15)
        val output = provider.updateNoId(testPat)
        assertTrue(output.created)
        val dbDoc = collection.find("id = :id").bind("id", "TESTINGIDNEW").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertNotNull(outputPat.birthDate)
    }

    @Test
    fun `delete test`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        provider.delete(IdType("TESTINGID5"))
        assertNull(collection.find("id = :id").bind("id", "TESTINGID5").execute().fetchOne())
    }

    @Test
    fun `read test`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        val output = provider.read(IdType("TESTINGID6"))
        assertEquals(output.birthDate, testPat.birthDate)
    }

    @Test
    fun `read test fail`() {
        assertThrows<ResourceNotFoundException> { provider.read(IdType("TESTINGIDBAD")) }
    }

    @Test
    fun `return all test`() {
        val testPat1 = Patient()
        testPat1.birthDate = Date(87, 0, 15)
        testPat1.id = "TESTINGID7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()
        val testPat2 = Patient()
        testPat2.birthDate = Date(87, 1, 15)
        testPat2.id = "TESTINGID8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = provider.returnAll()
        assertTrue(output.size >= 2)
    }

    @Test
    fun `birthday search test`() {
        val testPat1 = Patient()
        testPat1.birthDate = Date(87, 0, 15)
        testPat1.id = "TESTINGID9"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()
        val testPat2 = Patient()
        testPat2.birthDate = Date(87, 1, 15)
        testPat2.id = "TESTINGID10"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = provider.searchByBirth(DateParam("1987-01-15"))
        assertEquals(output[0].birthDate, testPat1.birthDate)
    }

    @Test
    fun `identifier search test`() {
        val testPat = Patient()
        testPat.id = "TESTINGIDENTIFIER"
        testPat.birthDate = Date(87, 0, 15)
        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testPat.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        val testPat2 = Patient()
        testPat2.id = "TESTINGIDENTIFIER2"
        testPat2.birthDate = Date(87, 1, 15)
        val identifier2 = Identifier()
        identifier2.value = "E1928341293"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testPat.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()
        val token = TokenParam()
        token.value = "E2731"
        token.system = "urn:oid:1.2.840.114350.1.1"
        val output = provider.searchByIdentifier(token)
        assertEquals(output.birthDate, testPat.birthDate)
    }

    @Test
    fun `code coverage test!`() {
        assertEquals(provider.resourceType, Patient::class.java)
    }
}
