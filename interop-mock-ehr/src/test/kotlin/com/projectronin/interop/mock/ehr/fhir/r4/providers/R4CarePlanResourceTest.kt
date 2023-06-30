package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CarePlanDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4CarePlanResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var carePlanProvider: R4CarePlanResourceProvider
    private lateinit var dao: R4CarePlanDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(CarePlan::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(CarePlan::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4CarePlanDAO(database, FhirContext.forR4())
        carePlanProvider = R4CarePlanResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(carePlanProvider.resourceType, CarePlan::class.java)
    }

    @Test
    fun `code cov`() {
        dao.searchByQuery("", "", null, null)
        dao.searchByQuery("", "", null)
        dao.searchByQuery("", "")
    }

    @Test
    fun `search by patient`() {
        val prefix = "1-"
        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        val testCarePlan2 = CarePlan()
        testCarePlan2.subject = Reference("Patient/${prefix}BADID")
        testCarePlan2.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan2.id = "${prefix}TESTPLAN2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan2)).execute()

        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1")
        )
        assertEquals(1, output.size)
        assertEquals("CarePlan/${testCarePlan1.id}", output[0].id)
    }

    @Test
    fun `search default returns null`() {
        val prefix = "2-"
        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}No")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1")
        )
        assertEquals(0, output.size)
    }

    @Test
    fun `search by date range - valid`() {
        val prefix = "3-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create second test CarePlan
        val testPeriod2 = Period()
        testPeriod2.start = Date(122, 8, 1)
        testPeriod2.end = Date(122, 9, 1)

        val testCarePlan2 = CarePlan()
        testCarePlan2.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan2.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan2.id = "${prefix}TESTPLAN2"
        testCarePlan2.period = testPeriod2

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan2)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setLowerBound("ge2022-07-01")
        dateRange.setUpperBound("le2022-08-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(1, output.size)
        assertEquals("CarePlan/${testCarePlan1.id}", output[0].id)
    }

    @Test
    fun `search by date range - not in range`() {
        val prefix = "4-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setLowerBound("ge2023-07-01")
        dateRange.setUpperBound("le2023-08-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(0, output.size)
    }

    @Test
    fun `search by date range - null search`() {
        val prefix = "5-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1")
        )

        // Check output
        assertEquals(1, output.size)
    }

    @Test
    fun `search by date range - null from date`() {
        val prefix = "6-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setUpperBound("le2023-08-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(1, output.size)
    }

    @Test
    fun `search by date range - null to date`() {
        val prefix = "7-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setLowerBound("ge2023-07-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(0, output.size)
    }

    @Test
    fun `search by date range - null period start`() {
        val prefix = "8-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.end = Date(122, 7, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setUpperBound("le2023-08-01")
        dateRange.setLowerBound("ge2023-07-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(0, output.size)
    }

    @Test
    fun `search by date range - null period end`() {
        val prefix = "9-"

        // Create first test CarePlan
        val testPeriod1 = Period()
        testPeriod1.start = Date(122, 6, 30)

        val testCarePlan1 = CarePlan()
        testCarePlan1.subject = Reference("Patient/${prefix}TESTINGID")
        testCarePlan1.category = listOf(CodeableConcept(Coding("system", "cat1", "display")))
        testCarePlan1.id = "${prefix}TESTPLAN1"
        testCarePlan1.period = testPeriod1

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCarePlan1)).execute()

        // Create DateRangeParam
        val dateRange = DateRangeParam()
        dateRange.setUpperBound("le2023-08-01")
        dateRange.setLowerBound("ge2023-07-01")

        // Execute search
        val output = carePlanProvider.search(
            patientReferenceParam = ReferenceParam("${prefix}TESTINGID"),
            categoryParam = TokenParam("cat1"),
            dateRangeParam = dateRange
        )

        // Check output
        assertEquals(1, output.size)
    }
}
