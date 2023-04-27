package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4LocationResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var locationProvider: R4LocationResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(Location::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Location::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        val dao = R4LocationDAO(database, FhirContext.forR4())
        locationProvider = R4LocationResourceProvider(dao)
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
