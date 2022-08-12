package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.Include
import ca.uhn.fhir.rest.param.ReferenceAndListParam
import ca.uhn.fhir.rest.param.ReferenceOrListParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4LocationDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PractitionerRoleDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4PractitionerRoleResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var practitionerRoleProvider: R4PractitionerRoleResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection("test")
        val database = mockk<Schema>()
        every { database.createCollection(PractitionerRole::class.simpleName, true) } returns collection
        every { database.createCollection(Location::class.simpleName, true) } returns collection
        every { database.createCollection(Practitioner::class.simpleName, true) } returns collection
        practitionerRoleProvider = R4PractitionerRoleResourceProvider(
            R4PractitionerRoleDAO(database, FhirContext.forR4()),
            R4LocationDAO(database, FhirContext.forR4()),
            R4PractitionerDAO(database, FhirContext.forR4())
        )
    }

    @Test
    fun `identifier search test`() {
        val testRole = PractitionerRole()
        testRole.id = "TESTINGIDENTIFIER"
        testRole.active = true

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testRole.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole)).execute()

        val testRole2 = PractitionerRole()
        testRole2.id = "TESTINGIDENTIFIER2"
        testRole2.active = false

        val identifier2 = Identifier()
        identifier2.value = "E1928341293"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testRole.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole2)).execute()

        val token = TokenParam()
        token.value = "E2731"
        token.system = "urn:oid:1.2.840.114350.1.1"
        val output = practitionerRoleProvider.searchByIdentifier(token)
        assertEquals(output?.active, testRole.active)
    }

    @Test
    fun `search by practitioner and location`() {
        val testRole = PractitionerRole()
        testRole.practitioner = Reference("Practitioner/IPMD")
        testRole.location.add(Reference("Location/1"))
        testRole.id = "TESTINGIDENTIFIER3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole)).execute()

        val testRole2 = PractitionerRole()
        testRole2.practitioner = Reference("Practitioner/BADID")
        testRole2.location.add(Reference("Location/2"))
        testRole2.id = "TESTINGIDENTIFIER4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole2)).execute()

        val output = practitionerRoleProvider.search(
            practitionerReferenceParam = ReferenceParam("IPMD"),
            locationReferenceParam = ReferenceAndListParam().addAnd(ReferenceOrListParam().add(ReferenceParam("1")))
        )
        assertEquals(1, output.size)
        assertEquals("PractitionerRole/${testRole.id}", output[0].id)
    }

    @Test
    fun `include resources works`() {
        val testRole = PractitionerRole()
        testRole.practitioner = Reference("Practitioner/IPMD")
        testRole.location.add(Reference("Location/3"))
        testRole.location.add(Reference("Location/4"))
        testRole.id = "TESTINGIDENTIFIER5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole)).execute()

        val location = Location()
        location.id = "3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(location)).execute()
        val location2 = Location()
        location2.id = "4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(location2)).execute()

        val practitioner = Practitioner()
        practitioner.id = "IPMD"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(practitioner)).execute()

        val output = practitionerRoleProvider.search(
            practitionerReferenceParam = ReferenceParam("IPMD"),
            locationReferenceParam = ReferenceAndListParam().addAnd(ReferenceOrListParam().add(ReferenceParam("3"))),
            includeSetParam = setOf(Include("PractitionerRole:practitioner"), Include("PractitionerRole:location"))
        )
        assertEquals("Practitioner/IPMD", ((output[0].practitioner.resource) as Practitioner).id)
        assertEquals("Location/3", ((output[0].location[0].resource) as Location).id)
        assertEquals("Location/4", ((output[0].location[1].resource) as Location).id)
    }

    @Test
    fun `include resources failure test`() {
        val testRole = PractitionerRole()
        testRole.practitioner = Reference("Practitioner/IPMD")
        testRole.location.add(Reference("Location/3"))
        testRole.location.add(Reference("Location/4"))
        testRole.id = "TESTINGIDENTIFIER5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole)).execute()

        assertThrows<InvalidParameterException> {
            practitionerRoleProvider.search(
                practitionerReferenceParam = ReferenceParam("IPMD"),
                locationReferenceParam = ReferenceAndListParam().addAnd(ReferenceOrListParam().add(ReferenceParam("3"))),
                includeSetParam = setOf(Include("PractitionerRole:somethingNotIncluded"))
            )
        }
    }

    @Test
    fun `include resources failure test can't find resources`() {
        val testRole = PractitionerRole()
        testRole.practitioner = Reference("Practitioner/IPMD2")
        testRole.location.add(Reference("Location/8"))
        testRole.location.add(Reference("Location/9"))
        testRole.id = "TESTINGIDENTIFIER5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRole)).execute()

        practitionerRoleProvider.search( // nothing to assert, just trying to catch the exceptions
            practitionerReferenceParam = ReferenceParam("IPMD2"),
            locationReferenceParam = ReferenceAndListParam().addAnd(ReferenceOrListParam().add(ReferenceParam("9"))),
            includeSetParam = setOf(Include("PractitionerRole:practitioner"), Include("PractitionerRole:location"))
        )
    }

    @Test
    fun `identifier search not found test`() {
        val token = TokenParam()
        token.value = "NotGoingToFindThisID"
        token.system = "BadSystem"
        val output = practitionerRoleProvider.searchByIdentifier(token)
        assertNull(output)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(practitionerRoleProvider.resourceType, PractitionerRole::class.java)
    }
}
