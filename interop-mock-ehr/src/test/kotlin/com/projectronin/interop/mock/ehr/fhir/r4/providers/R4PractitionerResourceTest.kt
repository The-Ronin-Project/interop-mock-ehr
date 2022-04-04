package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.MockEHRBaseTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
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
class R4PractitionerResourceTest : MockEHRBaseTest() {

    private lateinit var collection: Collection
    private lateinit var practitionerProvider: R4PractitionerResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(Practitioner::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Practitioner::class.simpleName, true) } returns collection
        practitionerProvider = R4PractitionerResourceProvider(R4PractitionerDAO(database))
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
        testPract.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPract2)).execute()

        val token = TokenParam()
        token.value = "E2731"
        token.system = "urn:oid:1.2.840.114350.1.1"
        val output = practitionerProvider.searchByIdentifier(token)
        assertEquals(output?.birthDate, testPract.birthDate)
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
}
