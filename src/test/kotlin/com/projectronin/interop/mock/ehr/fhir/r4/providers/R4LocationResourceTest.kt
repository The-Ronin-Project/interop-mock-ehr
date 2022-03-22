package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.getTestCollection
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4LocationResourceTest {

    private lateinit var collection: Collection
    private lateinit var locationProvider: R4LocationResourceProvider

    @BeforeAll
    fun initTest() {
        collection = getTestCollection()

        val database = mockk<Schema>()
        every { database.createCollection(Location::class.simpleName, true) } returns collection
        locationProvider = R4LocationResourceProvider(R4LocationDAO(database))
    }

    @Test
    fun `identifier search test`() {
        val testLocation = Location()
        testLocation.id = "TESTINGIDENTIFIER"
        testLocation.status = Location.LocationStatus.ACTIVE

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testLocation.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testLocation)).execute()

        val testLocation2 = Location()
        testLocation2.id = "TESTINGIDENTIFIER2"
        testLocation2.status = Location.LocationStatus.INACTIVE

        val identifier2 = Identifier()
        identifier2.value = "E1928341293"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testLocation.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testLocation2)).execute()

        val token = TokenParam()
        token.value = "E2731"
        token.system = "urn:oid:1.2.840.114350.1.1"
        val output = locationProvider.searchByIdentifier(token)
        assertEquals(output?.status, testLocation.status)
    }

    @Test
    fun `identifier search not found test`() {
        val token = TokenParam()
        token.value = "NotGoingToFindThisID"
        token.system = "BadSystem"
        val output = locationProvider.searchByIdentifier(token)
        assertNull(output)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(locationProvider.resourceType, Location::class.java)
    }
}
