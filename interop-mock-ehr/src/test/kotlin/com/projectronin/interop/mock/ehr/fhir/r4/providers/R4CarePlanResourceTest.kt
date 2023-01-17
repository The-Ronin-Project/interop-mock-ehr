package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CarePlanDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4CarePlanResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var carePlanProvider: R4CarePlanResourceProvider
    private lateinit var dao: R4CarePlanDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(CarePlan::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(CarePlan::class.simpleName, true) } returns collection
        dao = R4CarePlanDAO(database, FhirContext.forR4())
        carePlanProvider = R4CarePlanResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(carePlanProvider.resourceType, CarePlan::class.java)
    }

    @Test
    fun `search by patient`() {
        val prefix = "full-"
        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        val testCarePlan2 = CarePlan()
        testCarePlan2.subject = Reference("Patient/${prefix}BADID")
        testCarePlan2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan2)).execute()

        val output = carePlanProvider.search(patientReferenceParam = ReferenceParam("${prefix}TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("CarePlan/${testCarePlan1.id}", output[0].id)
    }

    @Test
    fun `search default returns null`() {
        val prefix = "full-"
        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}No")
        testCarePlan1.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        val output = carePlanProvider.search()
        assertEquals(0, output.size)
    }
}
