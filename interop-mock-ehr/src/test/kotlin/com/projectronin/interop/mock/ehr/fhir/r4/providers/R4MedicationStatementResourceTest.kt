package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationStatementDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationStatementResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var medicationStatementProvider: R4MedicationStatementResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationStatement::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(MedicationStatement::class.simpleName, true) } returns collection
        val dao = R4MedicationStatementDAO(database, FhirContext.forR4())
        medicationStatementProvider = R4MedicationStatementResourceProvider(dao)
    }

    @Test
    fun `patient search test`() {
        val testMedicationStatement = MedicationStatement()
        testMedicationStatement.id = "TESTINGIDENTIFIER"
        testMedicationStatement.subject = Reference().setReference("Patient/12345")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement)).execute()
        val testMedicationStatement2 = MedicationStatement()
        testMedicationStatement2.id = "TESTINGIDENTIFIER2"
        testMedicationStatement2.subject = Reference().setReference("Patient/67890")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement2)).execute()

        val token = ReferenceParam()
        token.value = "12345"
        val output = medicationStatementProvider.search(token)
        assertEquals(output.first().id.removePrefix("MedicationStatement/"), testMedicationStatement.id)
    }

    @Test
    fun `null test`() {
        val output = medicationStatementProvider.search(null)
        assertTrue(output.isEmpty())
        val output2 = medicationStatementProvider.search()
        assertTrue(output2.isEmpty())
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(medicationStatementProvider.resourceType, MedicationStatement::class.java)
    }
}
