package com.projectronin.interop.mock.ehr.stu3.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationStatementDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3MedicationStatementProvider
import com.projectronin.interop.mock.ehr.fhir.stu3.toR4
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev.SafeCollection
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.hl7.fhir.dstu3.model.IdType
import org.hl7.fhir.dstu3.model.MedicationStatement
import org.hl7.fhir.dstu3.model.Reference
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import org.hl7.fhir.r4.model.MedicationStatement as R4MedicationStatement

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class STU3MedicationStatementTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var medicationStatementProvider: STU3MedicationStatementProvider
    private lateinit var dao: R4MedicationStatementDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationStatement::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(R4MedicationStatement::class.java) } returns SafeCollection("resource", collection)
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4MedicationStatementDAO(database, FhirContext.forR4())
        medicationStatementProvider = STU3MedicationStatementProvider(dao)
    }

    @Test
    fun `insert test with id`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID1"

        val output = medicationStatementProvider.create(testMedicationStatement)
        assertEquals(output.id, IdType("TESTINGID1"))
    }

    @Test
    fun `insert test without id`() {
        val testMedicationStatement = MedicationStatement()

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val output = medicationStatementProvider.create(testMedicationStatement)
        assertEquals(output.id, IdType("UUID-GENERATED-ID"))
        unmockkStatic(UUID::class)
    }

    @Test
    fun `update test with id`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement.toR4())).execute()

        testMedicationStatement.status = MedicationStatement.MedicationStatementStatus.ACTIVE
        val output = medicationStatementProvider.update(IdType("TESTINGID2"), testMedicationStatement)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID2").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4MedicationStatement::class.java, dbDoc.toString())
        assertEquals(outputAppt.status, R4MedicationStatement.MedicationStatementStatus.ACTIVE)
    }

    @Test
    fun `update test without id`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement.toR4())).execute()
        testMedicationStatement.status = MedicationStatement.MedicationStatementStatus.ACTIVE

        val output = medicationStatementProvider.updateNoId(testMedicationStatement)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID3").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4MedicationStatement::class.java, dbDoc.toString())
        assertEquals(outputAppt.status, R4MedicationStatement.MedicationStatementStatus.ACTIVE)
    }

    @Test
    fun `update test not found so create new`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID4"
        val output = medicationStatementProvider.updateNoId(testMedicationStatement)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID4").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4MedicationStatement::class.java, dbDoc.toString())
        assertNotNull(outputAppt.id)
    }

    @Test
    fun `delete test`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement.toR4())).execute()

        medicationStatementProvider.delete(IdType("TESTINGID5"))
        Assertions.assertNull(collection.find("id = :id").bind("id", "TESTINGID5").execute().fetchOne())
    }

    @Test
    fun `read test`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGID6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement.toR4())).execute()

        val output = medicationStatementProvider.read(IdType("TESTINGID6"))
        assertEquals(output.id, "MedicationStatement/${testMedicationStatement.id}")
    }

    @Test
    fun `read test using multiple ids`() {
        val testMedicationStatement1 = MedicationStatement()
        testMedicationStatement1.id = "TESTINGIDMULTI1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement1.toR4())).execute()
        val testMedicationStatement2 = MedicationStatement()
        testMedicationStatement2.id = "TESTINGIDMULTI2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement2.toR4())).execute()

        val output = medicationStatementProvider.readMultiple(TokenOrListParam("", "TESTINGIDMULTI1", "TESTINGIDMULTI2"))
        assertEquals(output.size, 2)
    }

    @Test
    fun `read test all`() {
        val testMedicationStatement1 = MedicationStatement()
        testMedicationStatement1.id = "TESTINGID7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement1.toR4())).execute()
        val testMedicationStatement2 = MedicationStatement()
        testMedicationStatement2.id = "TESTINGID8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement2.toR4())).execute()
        val output = medicationStatementProvider.returnAll()
        assertTrue(output.size > 1)
    }

    @Test
    fun `search by patient`() {
        val testMedicationStatement1 = MedicationStatement()
        testMedicationStatement1.id = "TEST1"
        testMedicationStatement1.subject = Reference("Patient/TESTINGID1")
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testMedicationStatement1)).execute()

        val testMedicationStatement2 = MedicationStatement()
        testMedicationStatement2.id = "TEST2"
        testMedicationStatement2.subject = Reference("Patient/TESTINGID2")
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testMedicationStatement2)).execute()

        val output = medicationStatementProvider.search(patientReferenceParam = ReferenceParam("TESTINGID1"))
        assertEquals(1, output.size)
        assertEquals("MedicationStatement/${testMedicationStatement1.id}", output[0].id)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(medicationStatementProvider.resourceType, MedicationStatement::class.java)
    }

    @Test
    fun `medication statement code coverage test!`() {
        dao.searchByQuery() // tests default parameter values
    }
}
