package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.Include
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4PatientDAO
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException
import java.util.Date
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4PatientAndBaseResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var patientProvider: R4PatientResourceProvider
    private lateinit var dao: R4PatientDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Patient::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Patient::class.simpleName, true) } returns collection
        dao = R4PatientDAO(database)
        patientProvider = R4PatientResourceProvider(dao)
    }

    @Test
    fun `insert test with id`() {
        val testPat = Patient()
        testPat.id = "TESTINGID1"
        testPat.birthDate = Date(87, 0, 15)

        val output = patientProvider.create(testPat)
        assertEquals(output.id, IdType("TESTINGID1"))
    }

    @Test
    fun `insert test without id`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val output = patientProvider.create(testPat)
        assertEquals(output.id, IdType("UUID-GENERATED-ID"))
        unmockkStatic(UUID::class)
    }

    @Test
    fun `update test with id`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        testPat.active = true
        val output = patientProvider.update(IdType("TESTINGID2"), testPat)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID2").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertTrue(outputPat.active)
    }

    @Test
    fun `update test without id`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()
        testPat.active = true

        val output = patientProvider.updateNoId(testPat)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID3").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertTrue(outputPat.active)
    }

    @Test
    fun `update test not found so create new`() {
        val testPat = Patient()
        testPat.id = "TESTINGID4"
        testPat.birthDate = Date(87, 0, 15)
        val output = patientProvider.updateNoId(testPat)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID4").execute().fetchOne()
        val outputPat = FhirContext.forR4().newJsonParser().parseResource(Patient::class.java, dbDoc.toString())
        assertNotNull(outputPat.birthDate)
    }

    @Test
    fun `delete test`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        patientProvider.delete(IdType("TESTINGID5"))
        assertNull(collection.find("id = :id").bind("id", "TESTINGID5").execute().fetchOne())
    }

    @Test
    fun `read test`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        val output = patientProvider.read(IdType("TESTINGID6"))
        assertEquals(output.birthDate, testPat.birthDate)
    }

    @Test
    fun `read test using parameter`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        val output = patientProvider.readWithIncludes(IdType("TESTINGID7"), null)
        assertEquals(output.birthDate, testPat.birthDate)
    }

    @Test
    fun `include throws error`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        assertThrows<InvalidParameterException> {
            patientProvider.readWithIncludes(IdType("TESTINGID8"), setOf(Include("badInclude")))
        }
    }

    @Test
    fun `code coverage test`() {
        val testPat = Patient()
        testPat.birthDate = Date(87, 0, 15)
        testPat.id = "TESTINGID9"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat)).execute()

        val output = patientProvider.readWithIncludes(IdType("TESTINGID9"), setOf())
        assertEquals(output.birthDate, testPat.birthDate)
    }

    @Test
    fun `read test fail`() {
        assertThrows<ResourceNotFoundException> { patientProvider.read(IdType("TESTINGIDBAD")) }
    }

    @Test
    fun `return all test`() {
        val testPat1 = Patient()
        testPat1.birthDate = Date(87, 0, 15)
        testPat1.id = "TESTINGID10"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()

        val testPat2 = Patient()
        testPat2.birthDate = Date(87, 1, 15)
        testPat2.id = "TESTINGID11"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = patientProvider.returnAll()
        assertTrue(output.size >= 2)
    }

    @Test
    fun `birthday search test`() {
        val testPat1 = Patient()
        testPat1.birthDate = Date(87, 0, 15)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()

        val testPat2 = Patient()
        testPat2.birthDate = Date(87, 1, 15)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = patientProvider.search(DateParam("1987-01-15")).first()
        assertEquals(output.birthDate, testPat1.birthDate)
    }

    @Test
    fun `name and gender search test`() {
        val testPat1 = Patient()
        testPat1.addName().setFamily("testFamily").addGiven("testGiven")
        testPat1.gender = Enumerations.AdministrativeGender.MALE
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()

        val testPat2 = Patient()
        testPat2.addName().setFamily("badFamily").addGiven("badGiven")
        testPat2.gender = Enumerations.AdministrativeGender.FEMALE
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output =
            patientProvider.search(
                givenName = StringParam("testGiven"),
                familyName = StringParam("testFamily"),
                gender = StringParam("male")
            ).first()

        assertEquals(output.nameFirstRep.family, testPat1.nameFirstRep.family)
        assertEquals(output.gender, testPat1.gender)
    }

    @Test
    fun `email search test`() {
        val testPat1 = Patient()
        testPat1.telecom.add(
            ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("goodEmail@com.com")
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()

        val testPat2 = Patient()
        testPat2.telecom.add(
            ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("badEmail@com.com")
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = patientProvider.search(email = StringParam("goodEmail@com.com")).first()
        assertEquals(output.telecom.first().system, testPat1.telecom.first().system)
        assertEquals(output.telecom.first().value, testPat1.telecom.first().value)
    }

    @Test
    fun `phone search test`() {
        val testPat1 = Patient()
        testPat1.telecom.add(
            ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("608-608-6080")
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat1)).execute()

        val testPat2 = Patient()
        testPat2.telecom.add(
            ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("808-908-9090")
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testPat2)).execute()

        val output = patientProvider.search(telecomParam = TokenParam("608-608-6080")).first()
        assertEquals(output.telecom.first().system, testPat1.telecom.first().system)
        assertEquals(output.telecom.first().value, testPat1.telecom.first().value)
    }

    @Test
    fun `identifier search test`() {
        val testPat = Patient()
        testPat.id = "TESTINGIDENTIFIER"
        testPat.birthDate = Date(87, 0, 15)

        val identifier = Identifier()
        identifier.value = "E2731"
        identifier.system = "MRN"
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
        token.system = "MRN"
        val output = patientProvider.searchByIdentifier(token)
        assertEquals(output?.birthDate, testPat.birthDate)
    }

    @Test
    fun `identifier search not found test`() {
        val token = TokenParam()
        token.value = "NotGoingToFindThisID"
        token.system = "BadSystem"
        val output = patientProvider.searchByIdentifier(token)
        assertNull(output)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(patientProvider.resourceType, Patient::class.java)
    }

    @Test
    fun `cove coverage test`() {
        assertEquals(dao.searchByQuery().size, 0)
    }
}
