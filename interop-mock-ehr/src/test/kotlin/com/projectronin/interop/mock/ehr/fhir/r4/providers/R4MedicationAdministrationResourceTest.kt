package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationAdministrationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationAdministrationResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var medicationAdministrationProvider: R4MedicationAdministrationResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationAdministration::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(MedicationAdministration::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        val dao = R4MedicationAdministrationDAO(database, FhirContext.forR4())
        medicationAdministrationProvider = R4MedicationAdministrationResourceProvider(dao)
    }

    @Test
    fun `identifier search test`() {
        val testMedicationAdministration = MedicationAdministration()
        testMedicationAdministration.id = "TESTINGIDENTIFIER"
        testMedicationAdministration.status = MedicationAdministration.MedicationAdministrationStatus.COMPLETED
        testMedicationAdministration.request = Reference("MedicationRequest/MedRequestID1")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationAdministration)).execute()

        val testMedicationAdministration2 = MedicationAdministration()
        testMedicationAdministration2.id = "TESTINGIDENTIFIER2"
        testMedicationAdministration2.status = MedicationAdministration.MedicationAdministrationStatus.STOPPED
        testMedicationAdministration2.request = Reference("MedicationRequest/MedRequestID2")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationAdministration2)).execute()

        val token = ReferenceParam()
        token.value = "MedRequestID1"
        val output = medicationAdministrationProvider.search(token)
        assertEquals(output.first().status, testMedicationAdministration.status)
    }

    @Test
    fun `identifier search not found test`() {
        val token = ReferenceParam()
        token.value = "BadRequest1"
        val output = medicationAdministrationProvider.search(token)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(medicationAdministrationProvider.resourceType, MedicationAdministration::class.java)
    }
}
