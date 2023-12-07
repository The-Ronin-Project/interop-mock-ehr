package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationRequestDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Dosage
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationRequestResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var medicationRequestProvider: R4MedicationRequestResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationRequest::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(MedicationRequest::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
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
    fun `search by date range using only lower bound`() {
        val testPatientId = "98765"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest.id = "TESTMEDREQ"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest2 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest2.id = "TESTMEDREQ2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest2.id}", output.first().id)
        assertEquals(115, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `search by date range using only upper bound`() {
        val testPatientId = "14875"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest3 =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest3.id = "TESTMEDREQ3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest3)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest4 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest4.id = "TESTMEDREQ4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest4)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest3.id}", output.first().id)
        assertEquals(110, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `search by date range using both bounds`() {
        val testPatientId = "88654"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest5 =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest5.id = "TESTMEDREQ5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest5)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest6 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest6.id = "TESTMEDREQ6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest6)).execute()

        val period3 = Period()
        period3.start = Date(120, 0, 10)
        val dosage3 = Dosage()
        dosage3.timing.repeat.bounds = period3
        val testMedicationRequest7 =
            MedicationRequest()
                .addDosageInstruction(dosage3)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest7.id = "TESTMEDREQ7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest7)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2017-02-22T13:12:00-06:00")
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest6.id}", output.first().id)
        assertEquals(115, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `search by date range with cerner param using lower bound`() {
        val testPatientId = "11566"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest8 =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest8.id = "TESTMEDREQ8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest8)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest9 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest9.id = "TESTMEDREQ9"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest9)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                cernerDateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest9.id}", output.first().id)
    }

    @Test
    fun `retrieve multiple results with date param`() {
        val testPatientId = "998507"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest12 =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest12.id = "TESTMEDREQ12"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest12)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest13 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest13.id = "TESTMEDREQ13"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest13)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2020-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(2, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest12.id}", output.first().id)
        assertEquals(110, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
        assertEquals("MedicationRequest/${testMedicationRequest13.id}", output.last().id)
        assertEquals(115, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `retrieve multiple results with cerner date param`() {
        val testPatientId = "114114"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val testMedicationRequest14 =
            MedicationRequest()
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest14.id = "TESTMEDREQ14"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest14)).execute()

        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val testMedicationRequest15 =
            MedicationRequest()
                .addDosageInstruction(dosage2)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest15.id = "TESTMEDREQ15"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest15)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2020-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                cernerDateRangeParam = dateParam,
            )
        assertEquals(2, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest14.id}", output.first().id)
        assertEquals(110, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
        assertEquals("MedicationRequest/${testMedicationRequest15.id}", output.last().id)
        assertEquals(115, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.last().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `retrieve results with date param when some dosage periods are not in date range`() {
        val testPatientId = "622232"
        val period = Period()
        period.start = Date(110, 0, 10)
        val dosage = Dosage()
        dosage.timing.repeat.bounds = period
        val period2 = Period()
        period2.start = Date(115, 0, 10)
        val dosage2 = Dosage()
        dosage2.timing.repeat.bounds = period2
        val period3 = Period()
        period3.start = Date(120, 0, 10)
        val dosage3 = Dosage()
        dosage3.timing.repeat.bounds = period3
        val testMedicationRequest16 =
            MedicationRequest()
                .addDosageInstruction(dosage3)
                .addDosageInstruction(dosage2)
                .addDosageInstruction(dosage)
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationRequest16.id = "TESTMEDREQ16"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationRequest16)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2013-02-22T13:12:00-06:00")

        val output =
            medicationRequestProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationRequest/${testMedicationRequest16.id}", output.first().id)
        assertEquals(120, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.first().timing.repeat.boundsPeriod.start.date)
        assertEquals(115, output.first().dosageInstruction[1].timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction[1].timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction[1].timing.repeat.boundsPeriod.start.date)
        assertEquals(110, output.first().dosageInstruction.last().timing.repeat.boundsPeriod.start.year)
        assertEquals(0, output.first().dosageInstruction.last().timing.repeat.boundsPeriod.start.month)
        assertEquals(10, output.first().dosageInstruction.last().timing.repeat.boundsPeriod.start.date)
    }

    @Test
    fun `mixing cerner and non-cerner params throw error`() {
        assertThrows(UnsupportedOperationException::class.java) {
            medicationRequestProvider.search(
                dateRangeParam = DateRangeParam(),
                cernerDateRangeParam = DateRangeParam(),
            )
        }
    }

    @Test
    fun `date params without patient param returns empty list`() {
        val output =
            medicationRequestProvider.search(
                dateRangeParam = DateRangeParam(),
            )
        assertEquals(0, output.size)

        val output2 =
            medicationRequestProvider.search(
                cernerDateRangeParam = DateRangeParam(),
            )
        assertEquals(0, output2.size)
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
