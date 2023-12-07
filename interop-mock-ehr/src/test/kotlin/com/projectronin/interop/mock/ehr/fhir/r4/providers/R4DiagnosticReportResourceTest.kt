package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DiagnosticReportDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4DiagnosticReportResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var diagnosticReportProvider: R4DiagnosticReportResourceProvider

    @BeforeAll
    fun beforeTest() {
        collection = createCollection(DiagnosticReport::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(DiagnosticReport::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        val dao = R4DiagnosticReportDAO(database, FhirContext.forR4())
        diagnosticReportProvider = R4DiagnosticReportResourceProvider(dao)
    }

    @BeforeEach
    fun clearCollection() {
        collection.remove("true").execute() // clear collection from previous tests
    }

    @Test
    fun `empty test`() {
        val emptyRef = ReferenceParam("")
        val output = diagnosticReportProvider.search(emptyRef)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(diagnosticReportProvider.resourceType, DiagnosticReport::class.java)
    }

    @Test
    fun `date params without patient param returns empty list`() {
        val emptyRef = ReferenceParam("")
        val output =
            diagnosticReportProvider.search(
                patient = emptyRef,
                dateRangeParam = DateRangeParam(),
            )
        assertEquals(0, output.size)

        val output2 =
            diagnosticReportProvider.search(
                patient = emptyRef,
                cernerDateRangeParam = DateRangeParam(),
            )
        assertEquals(0, output2.size)
    }

    @Test
    fun `mixing cerner and non-cerner params throw error`() {
        val emptyRef = ReferenceParam()
        assertThrows(UnsupportedOperationException::class.java) {
            diagnosticReportProvider.search(
                patient = emptyRef,
                dateRangeParam = DateRangeParam(),
                cernerDateRangeParam = DateRangeParam(),
            )
        }
    }

    @Test
    fun `search by works`() {
        val diagnosticReport = DiagnosticReport()
        diagnosticReport.id = "DIAGNOSTICREPORT"
        diagnosticReport.subject = Reference().setReference("Patient/12345678")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport)).execute()
        val diagnosticReport2 = DiagnosticReport()
        diagnosticReport2.id = "DIAGNOSTICREPORT2"
        diagnosticReport2.subject = Reference().setReference("Patient/87654321")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport2)).execute()

        val refToken = ReferenceParam()
        refToken.value = "12345678"
        val output = diagnosticReportProvider.search(refToken)
        assertEquals(output.first().id.removePrefix("DiagnosticReport/"), diagnosticReport.id)
    }

    @Test
    fun `search by date using lower bound`() {
        val patientId = "1234"
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now: LocalDate = LocalDate.now()
        val dateDx = dateFormat.format(now.minus(Period.ofDays(8)))
        val diagnosticReport =
            DiagnosticReport()
                .setEffective(DateTimeType("${dateDx}T00:00:00.000Z"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport.id = "dx1234"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport)).execute()

        val diagnosticReport2 =
            DiagnosticReport()
                .setEffective(DateTimeType("${dateDx}T00:00:00.000Z"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport2.id = "dx12345678"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport2)).execute()

        val dataParam = DateRangeParam()
        dataParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            diagnosticReportProvider.search(
                patient = ReferenceParam(patientId),
                dateRangeParam = dataParam,
            )
        assertEquals(2, output.size)
        assertEquals("DiagnosticReport/${diagnosticReport.id}", output[0].id)
        assertEquals("DiagnosticReport/${diagnosticReport2.id}", output[1].id)
        assertNotNull(output[0].effective)
        assertNotNull(output[1].effective)
        assertEquals(output[0].effective.toString(), diagnosticReport.effective.toString())
        assertEquals(output[1].effective.toString(), diagnosticReport2.effective.toString())
    }

    @Test
    fun `search by date using upper bound`() {
        val patientId = "1234"
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now: LocalDate = LocalDate.now()
        val dateDx = dateFormat.format(now.minus(Period.ofDays(8)))
        val diagnosticReport =
            DiagnosticReport()
                .setEffective(DateTimeType("2010-02-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport.id = "dx1234"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport)).execute()

        val diagnosticReport2 =
            DiagnosticReport()
                .setEffective(DateTimeType("${dateDx}T00:00:00.000Z"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport2.id = "dx12345678"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport2)).execute()

        val dataParam = DateRangeParam()
        dataParam.upperBound = DateParam("2011-02-22T13:12:00-06:00")

        val output =
            diagnosticReportProvider.search(
                patient = ReferenceParam(patientId),
                dateRangeParam = dataParam,
            )
        assertEquals(1, output.size)
        assertEquals("DiagnosticReport/${diagnosticReport.id}", output[0].id)
        assertEquals(output[0].effective.toString(), diagnosticReport.effective.toString())
    }

    @Test
    fun `search by with upper and lower bound date`() {
        val patientId = "1234"
        val diagnosticReport =
            DiagnosticReport() // should not find this one
                .setEffective(DateTimeType("2018-02-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport.id = "dx1234"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport)).execute()

        val diagnosticReport2 =
            DiagnosticReport()
                .setEffective(DateTimeType("2020-04-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport2.id = "dx12345678"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport2)).execute()

        val diagnosticReport3 =
            DiagnosticReport()
                .setEffective(DateTimeType("2022-08-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport3.id = "dx87654321"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport3)).execute()

        val dataParam = DateRangeParam()
        dataParam.upperBound = DateParam("2023-02-22T13:12:00-06:00")
        dataParam.lowerBound = DateParam("2020-02-22T13:12:00-06:00")

        val output =
            diagnosticReportProvider.search(
                patient = ReferenceParam(patientId),
                dateRangeParam = dataParam,
            )
        assertEquals(2, output.size)
        assertEquals("DiagnosticReport/${diagnosticReport2.id}", output[0].id)
        assertEquals(output[0].effective.toString(), diagnosticReport2.effective.toString())
        assertEquals("DiagnosticReport/${diagnosticReport3.id}", output[1].id)
        assertEquals(output[1].effective.toString(), diagnosticReport3.effective.toString())
    }

    @Test
    fun `search by with cerner date param - upper`() {
        val patientId = "1234"
        val diagnosticReport =
            DiagnosticReport() // should not find this one
                .setEffective(DateTimeType("2018-02-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport.id = "dx1234"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport)).execute()

        val diagnosticReport2 =
            DiagnosticReport()
                .setEffective(DateTimeType("2020-04-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport2.id = "dx12345678"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport2)).execute()

        val diagnosticReport3 =
            DiagnosticReport()
                .setEffective(DateTimeType("2022-08-22T08:12:00-06:00"))
                .setSubject(
                    Reference().setReference(
                        "Patient/$patientId",
                    ),
                )
        diagnosticReport3.id = "dx87654321"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(diagnosticReport3)).execute()

        val dataParam = DateRangeParam()
        dataParam.lowerBound = DateParam("2020-02-22T13:12:00-06:00")

        val output =
            diagnosticReportProvider.search(
                patient = ReferenceParam(patientId),
                cernerDateRangeParam = dataParam,
            )
        assertEquals(2, output.size)
        assertEquals("DiagnosticReport/${diagnosticReport2.id}", output[0].id)
        assertEquals(output[0].effective.toString(), diagnosticReport2.effective.toString())
        assertEquals("DiagnosticReport/${diagnosticReport3.id}", output[1].id)
        assertEquals(output[1].effective.toString(), diagnosticReport3.effective.toString())
    }
}
