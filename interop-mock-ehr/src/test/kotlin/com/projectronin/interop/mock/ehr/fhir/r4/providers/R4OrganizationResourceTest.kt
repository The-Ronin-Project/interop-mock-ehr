package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4OrganizationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Organization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4OrganizationResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var organizationProvider: R4OrganizationResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(Organization::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Organization::class.java) } returns SafeXDev.SafeCollection(collection)
        val dao = R4OrganizationDAO(database, FhirContext.forR4())
        organizationProvider = R4OrganizationResourceProvider(dao)
    }

    @Test
    fun `read test`() {
        val testOrganization = Organization()
        testOrganization.id = "TESTINGIDENTIFIER1"

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization.addIdentifier(identifier)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization)).execute()

        val output = organizationProvider.read(IdType("TESTINGIDENTIFIER1"))
        val ident = output.identifier[0]
        assertEquals("E2731", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `read not found test`() {
        val testOrganization = Organization()
        testOrganization.id = "TESTINGIDENTIFIER"

        val exception = assertThrows<ResourceNotFoundException> {
            organizationProvider.read(IdType("NotGoingToFindThisID"))
        }
        assertEquals("No resource found with id: NotGoingToFindThisID", exception.message)
    }

    @Test
    fun `search one or more ids - multiple ids - some, but not all, ids are present in data`() {
        val testOrganization = Organization()
        testOrganization.id = "TESTINGIDENTIFIER2"

        val identifier = Identifier()
        identifier.value = "E27312"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization.addIdentifier(identifier)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization)).execute()

        val testOrganization3 = Organization()
        testOrganization3.id = "TESTINGIDENTIFIER3"

        val identifier3 = Identifier()
        identifier3.value = "E19283412933"
        identifier3.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization3.addIdentifier(identifier3)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization3)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER9"))
        params.add(StringParam("TESTINGIDENTIFIER2"))

        val output: List<Organization> = organizationProvider.searchByQuery(idListParam = params)
        assertEquals(1, output.size)
        val ident = output[0].identifier[0]
        assertEquals("E27312", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `search one or more ids - multiple ids - all ids are present`() {
        val testOrganization = Organization()
        testOrganization.id = "TESTINGIDENTIFIER4"

        val identifier = Identifier()
        identifier.value = "E27314"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization.addIdentifier(identifier)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization)).execute()

        val testOrganization2 = Organization()
        testOrganization2.id = "TESTINGIDENTIFIER5"

        val identifier2 = Identifier()
        identifier2.value = "E19283412935"
        identifier2.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization2.addIdentifier(identifier2)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization2)).execute()

        val testOrganization3 = Organization()
        testOrganization3.id = "TESTINGIDENTIFIER6"

        val identifier3 = Identifier()
        identifier3.value = "E19286"
        identifier3.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization3.addIdentifier(identifier3)

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization3)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER4"))
        params.add(StringParam("TESTINGIDENTIFIER5"))
        params.add(StringParam("TESTINGIDENTIFIER6"))

        val output = organizationProvider.searchByQuery(idListParam = params)
        assertEquals(3, output.size)
        val ident1 = output[0].identifier[0]
        assertEquals("E27314", ident1?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident1?.system)
        val ident2 = output[1].identifier[0]
        assertEquals("E19283412935", ident2?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident2?.system)
        val ident3 = output[2].identifier[0]
        assertEquals("E19286", ident3?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident3?.system)
    }

    @Test
    fun `search one or more ids - one id`() {
        val testOrganization = Organization()
        testOrganization.id = "TESTINGIDENTIFIER7"

        val identifier = Identifier()
        identifier.value = "E27317"
        identifier.system = "urn:oid:1.2.840.114350.1.1"
        testOrganization.addIdentifier(identifier)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testOrganization)).execute()

        val params = StringOrListParam()
        params.add(StringParam("TESTINGIDENTIFIER7"))

        val output = organizationProvider.searchByQuery(idListParam = params)

        assertEquals(1, output.size)
        val ident = output[0].identifier[0]
        assertEquals("E27317", ident?.value)
        assertEquals("urn:oid:1.2.840.114350.1.1", ident?.system)
    }

    @Test
    fun `search one or more ids - empty list input`() {
        val output = organizationProvider.searchByQuery(idListParam = StringOrListParam())
        assertEquals(0, output.size)
    }

    @Test
    fun `search one or more ids - null list input`() {
        val output = organizationProvider.searchByQuery(idListParam = null)
        assertEquals(0, output.size)
    }

    @Test
    fun `search one or more ids - no input`() {
        val output = organizationProvider.searchByQuery()
        assertEquals(0, output.size)
    }

    @Test
    fun `search one or more ids - empty param input`() {
        val params = StringOrListParam()
        params.add(StringParam())
        val output = organizationProvider.searchByQuery(idListParam = params)
        assertEquals(0, output.size)
    }

    @Test
    fun `search one or more ids - null param input`() {
        val params = StringOrListParam()
        params.add(null)
        val output = organizationProvider.searchByQuery(idListParam = params)
        assertEquals(0, output.size)
    }

    @Test
    fun `search one or more ids - null param value input`() {
        val params = StringOrListParam()
        params.add(StringParam(null))
        val output = organizationProvider.searchByQuery(idListParam = params)
        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(organizationProvider.resourceType, Organization::class.java)
    }
}
