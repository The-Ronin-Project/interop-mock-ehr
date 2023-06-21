package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4RequestGroupDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.RequestGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4RequestGroupResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var requestGroupProvider: R4RequestGroupResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(RequestGroup::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(RequestGroup::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        val dao = R4RequestGroupDAO(database, FhirContext.forR4())
        requestGroupProvider = R4RequestGroupResourceProvider(dao)
    }

    @Test
    fun `correct resource is returned`() {
        assertEquals(requestGroupProvider.resourceType, RequestGroup::class.java)
    }

    @Test
    fun `read works`() {
        val testRequestGroup = RequestGroup()
        testRequestGroup.id = "TESTINGIDENTIFIER1"
        val identifier = Identifier()
        identifier.value = "E22222222"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup.addIdentifier(identifier)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup)).execute()

        val output = requestGroupProvider.read(IdType("TESTINGIDENTIFIER1"))
        val ident = output.identifier[0]
        assertEquals("E22222222", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `no resource found works`() {
        val testRequestGroup = RequestGroup()
        testRequestGroup.id = "TESTINGIDENTIFIER1"

        val exception = assertThrows<ResourceNotFoundException> {
            requestGroupProvider.read(IdType("NothingToSeeHere"))
        }
        assertEquals("No resource found with id: NothingToSeeHere", exception.message)
    }

    @Test
    fun `search works with multiple ids and all ids found`() {
        val testRequestGroup = RequestGroup()
        testRequestGroup.id = "TESTINGIDENTIFIER1"
        val identifier = Identifier()
        identifier.value = "E22222222"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup)).execute()

        val testRequestGroup2 = RequestGroup()
        testRequestGroup2.id = "TESTINGIDENTIFIER2"
        val identifier2 = Identifier()
        identifier2.value = "E12345678"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup2.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup2)).execute()

        val testRequestGroup3 = RequestGroup()
        testRequestGroup3.id = "TESTINGIDENTIFIER3"
        val identifier3 = Identifier()
        identifier3.value = "E87654321"
        identifier3.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup3.addIdentifier(identifier3)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup3)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER1"))
        params.add(StringParam("TESTINGIDENTIFIER2"))
        params.add(StringParam("TESTINGIDENTIFIER3"))

        val output = requestGroupProvider.searchByQuery(idListParam = params)
        assertEquals(3, output.size)
        val ident = output[0].identifier[0]
        assertEquals("E22222222", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
        val ident2 = output[1].identifier[0]
        assertEquals("E12345678", ident2?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
        val ident3 = output[2].identifier[0]
        assertEquals("E87654321", ident3?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `search works with multiple ids but NOT all ids found`() {
        val testRequestGroup = RequestGroup()
        testRequestGroup.id = "TESTINGIDENTIFIER1"
        val identifier = Identifier()
        identifier.value = "E22222222"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup)).execute()

        val testRequestGroup2 = RequestGroup()
        testRequestGroup2.id = "TESTINGIDENTIFIER2"
        val identifier2 = Identifier()
        identifier2.value = "E12345678"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup2.addIdentifier(identifier2)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup2)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER1"))
        params.add(StringParam("TESTINGIDENTIFIER2"))
        params.add(StringParam("TESTINGIDENTIFIER3"))

        val output = requestGroupProvider.searchByQuery(idListParam = params)
        assertEquals(2, output.size)
        val ident = output[0].identifier[0]
        assertEquals("E22222222", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
        val ident2 = output[1].identifier[0]
        assertEquals("E12345678", ident2?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `search works with one id`() {
        val testRequestGroup = RequestGroup()
        testRequestGroup.id = "TESTINGIDENTIFIER1"
        val identifier = Identifier()
        identifier.value = "E22222222"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testRequestGroup.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testRequestGroup)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER1"))

        val output = requestGroupProvider.searchByQuery(idListParam = params)
        assertEquals(1, output.size)
        val ident = output[0].identifier[0]
        assertEquals("E22222222", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `search with empty list works`() {
        val output = requestGroupProvider.searchByQuery(idListParam = StringOrListParam())
        assertEquals(0, output.size)
    }

    @Test
    fun `search with null value list works`() {
        val output = requestGroupProvider.searchByQuery(idListParam = null)
        assertEquals(0, output.size)
    }

    @Test
    fun `search with no input works`() {
        val output = requestGroupProvider.searchByQuery()
        assertEquals(0, output.size)
    }

    @Test
    fun `search works with empty param input`() {
        val params = StringOrListParam()
        params.add(StringParam())
        val output = requestGroupProvider.searchByQuery(idListParam = params)
        assertEquals(0, output.size)
    }

    @Test
    fun `search works with null param input`() {
        val params = StringOrListParam()
        params.add(null)
        val output = requestGroupProvider.searchByQuery(idListParam = params)
        assertEquals(0, output.size)
    }
}
