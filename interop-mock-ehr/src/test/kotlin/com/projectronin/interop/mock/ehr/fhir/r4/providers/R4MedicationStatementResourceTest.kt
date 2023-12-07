package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationStatementDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationStatementResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var medicationStatementProvider: R4MedicationStatementResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(MedicationStatement::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(MedicationStatement::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
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
    fun `search by date range using only lower bound`() {
        val testPatientId = "98765"
        val testMedicationStatement3 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(110, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement3.setId("MEDSTMNT3")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement3)).execute()

        val testMedicationStatement4 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(115, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement4.setId("MEDSTMNT4")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement4)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationStatementProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationStatement/${testMedicationStatement4.id}", output.first().id)
        assertEquals(115, output.first().effectivePeriod.start.year)
        assertEquals(0, output.first().effectivePeriod.start.month)
        assertEquals(10, output.first().effectivePeriod.start.date)
    }

    @Test
    fun `search by date range using only upper bound`() {
        val testPatientId = "98741"
        val testMedicationStatement5 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(110, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement5.setId("MEDSTMNT5")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement5)).execute()

        val testMedicationStatement6 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(115, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement6.setId("MEDSTMNT6")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement6)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationStatementProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationStatement/${testMedicationStatement5.id}", output.first().id)
        assertEquals(110, output.first().effectivePeriod.start.year)
        assertEquals(0, output.first().effectivePeriod.start.month)
        assertEquals(10, output.first().effectivePeriod.start.date)
    }

    @Test
    fun `search by date range using both bounds`() {
        val testPatientId = "55541"
        val testMedicationStatement7 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(110, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement7.setId("MEDSTMNT7")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement7)).execute()

        val testMedicationStatement8 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(115, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement8.setId("MEDSTMNT8")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement8)).execute()

        val testMedicationStatement9 =
            MedicationStatement()
                .setEffective(
                    Period().setStart(
                        Date(120, 0, 10),
                    ),
                )
                .setSubject(
                    Reference().setReference(
                        "Patient/$testPatientId",
                    ),
                )
        testMedicationStatement9.setId("MEDSTMNT9")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testMedicationStatement9)).execute()

        val dateParam = DateRangeParam()
        dateParam.upperBound = DateParam("2017-02-22T13:12:00-06:00")
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            medicationStatementProvider.search(
                patientReferenceParam = ReferenceParam(testPatientId),
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("MedicationStatement/${testMedicationStatement8.id}", output.first().id)
        assertEquals(115, output.first().effectivePeriod.start.year)
        assertEquals(0, output.first().effectivePeriod.start.month)
        assertEquals(10, output.first().effectivePeriod.start.date)
    }

    @Test
    fun `date param without patient param returns empty list`() {
        val output =
            medicationStatementProvider.search(
                dateRangeParam = DateRangeParam(),
            )

        assertEquals(0, output.size)
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
