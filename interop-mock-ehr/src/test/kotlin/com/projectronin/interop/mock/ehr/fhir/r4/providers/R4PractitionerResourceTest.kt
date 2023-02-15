package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4PractitionerResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var practitionerProvider: R4PractitionerResourceProvider
    private lateinit var dao: R4PractitionerDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Practitioner::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Practitioner::class.java) } returns SafeXDev.SafeCollection(collection)
        dao = R4PractitionerDAO(database, FhirContext.forR4())
        practitionerProvider = R4PractitionerResourceProvider(dao)
    }

    @Test
    fun `identifier search test`() {
        val testPract = Practitioner()
        testPract.id = "TESTINGIDENTIFIER"
        testPract.birthDate = Date(87, 0, 15)

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testPract.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract)).execute()

        val testPract2 = Practitioner()
        testPract2.id = "TESTINGIDENTIFIER2"
        testPract2.birthDate = Date(87, 1, 15)

        val identifier2 = Identifier()
        identifier2.value = "E1928341293"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testPract2.addIdentifier(identifier2)
        val identifier3 = Identifier()
        identifier3.value = "E2731"
        identifier3.system = "NotTheSame"
        testPract2.addIdentifier(identifier3)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract2)).execute()

        val token = TokenParam()
        token.value = "E2731"
        token.system = "urn:oid:1.2.840.114350.1.1"
        val output = practitionerProvider.searchByIdentifier(token)
        assertEquals(output?.birthDate, testPract.birthDate)
    }

    @Test
    fun `identifier search without system returns null when duplicates`() {
        val testPract = Practitioner()
        testPract.id = "TESTINGIDENTIFIER"
        testPract.birthDate = Date(87, 0, 15)

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testPract.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract)).execute()

        val testPract2 = Practitioner()
        testPract2.id = "TESTINGIDENTIFIER2"
        testPract2.birthDate = Date(87, 1, 15)

        val identifier2 = Identifier()
        identifier2.value = "E2731"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testPract2.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract2)).execute()

        val token = TokenParam()
        token.value = "E2731"
        token.system = null
        val output = practitionerProvider.searchByIdentifier(token)
        assertNull(output)
    }

    @Test
    fun `identifier search not found test`() {
        val token = TokenParam()
        token.value = "NotGoingToFindThisID"
        token.system = "BadSystem"
        val output = practitionerProvider.searchByIdentifier(token)
        assertNull(output)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(practitionerProvider.resourceType, Practitioner::class.java)
    }

    @Test
    fun `baseDAO findById test`() {
        val practitioner = Practitioner()
        practitioner.id = "TESTINGFINDID"
        practitioner.birthDate = Date(87, 0, 15)
        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        practitioner.addIdentifier(identifier)
        practitioner.active = true

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(practitioner)).execute()

        var output = dao.findById("TESTINGFINDID")
        assertEquals(output.active, practitioner.active)
        assertEquals(output.identifier.get(0).value, practitioner.identifier.get(0).value)
        assertEquals(output.identifier.get(0).system, practitioner.identifier.get(0).system)
        assertEquals(output.birthDate, practitioner.birthDate)

        collection.remove("true").execute() // Clear the collection in case other tests run first
        var message = try {
            dao.findById("TESTINGFINDID")
        } catch (e: ResourceNotFoundException) {
            e.message
        }
        assertEquals(message, "No resource found with id: TESTINGFINDID")
    }

    @Test
    fun `dao code coverage`() {
        val testPract = Practitioner()
        testPract.id = "TESTINGIDENTIFIER"
        testPract.birthDate = Date(87, 0, 15)

        val identifier1 = Identifier()
        identifier1.value = "E2731"
        identifier1.type.text = "External"
        identifier1.system = "urn:oid:1.2.840.114350.1.1"
        testPract.addIdentifier(identifier1)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract)).execute()

        val testPract2 = Practitioner()
        testPract2.id = "TESTINGIDENTIFIER2"
        testPract2.birthDate = Date(87, 1, 15)

        val identifier2 = Identifier()
        identifier2.value = "E1928341293"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testPract2.addIdentifier(identifier2)
        val identifier3 = Identifier()
        identifier3.value = "E2731"
        identifier3.system = "NotTheSame"
        testPract2.addIdentifier(identifier3)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract2)).execute()
        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.type.text = "External"
        val output = dao.searchByIdentifier(identifier)
        assertEquals(output?.birthDate, testPract.birthDate)
    }
}
