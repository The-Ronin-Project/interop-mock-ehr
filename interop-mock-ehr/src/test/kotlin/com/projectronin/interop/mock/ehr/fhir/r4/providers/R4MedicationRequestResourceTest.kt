package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationRequestDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationRequestResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var medicationRequestProvider: R4MedicationRequestResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationRequest::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(MedicationRequest::class.java) } returns SafeXDev.SafeCollection(collection)
        val dao = R4MedicationRequestDAO(database, FhirContext.forR4())
        medicationRequestProvider = R4MedicationRequestResourceProvider(dao)
    }

    @Test
    fun `patient search test`() {
        val testMedicationRequest = MedicationRequest()
        testMedicationRequest.id = "TESTINGIDENTIFIER"
        testMedicationRequest.subject = Reference().setReference("Patient/12345")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest)).execute()
        val testMedicationRequest2 = MedicationRequest()
        testMedicationRequest2.id = "TESTINGIDENTIFIER2"
        testMedicationRequest2.subject = Reference().setReference("Patient/67890")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest2)).execute()

        val token = ReferenceParam()
        token.value = "12345"
        val output = medicationRequestProvider.search(token)
        assertEquals(output.first().id.removePrefix("MedicationRequest/"), testMedicationRequest.id)
    }

    @Test
    fun `null test`() {
        val output = medicationRequestProvider.search(null)
        assertTrue(output.isEmpty())
        val output2 = medicationRequestProvider.search()
        assertTrue(output2.isEmpty())
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(medicationRequestProvider.resourceType, MedicationRequest::class.java)
    }
}
