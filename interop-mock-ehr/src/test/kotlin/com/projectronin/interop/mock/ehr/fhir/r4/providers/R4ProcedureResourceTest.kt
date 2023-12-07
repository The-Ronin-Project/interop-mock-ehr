package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ProcedureDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4ProcedureResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var procedureProvider: R4ProcedureResourceProvider
    private lateinit var dao: R4ProcedureDAO

    @BeforeAll
    fun beforeTest() {
        collection = createCollection(Procedure::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every {
            database.createCollection(Procedure::class.java)
        } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every {
            database.run(any(), captureLambda<Collection.() -> Any>())
        } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4ProcedureDAO(database, FhirContext.forR4())
        procedureProvider = R4ProcedureResourceProvider(dao)
    }

    @BeforeEach
    fun clearCollection() {
        collection.remove("true").execute() // clear collection from previous tests
    }

    @Test
    fun `correct resource type returned`() {
        assertEquals(procedureProvider.resourceType, Procedure::class.java)
    }

    @Test
    fun `search by patient`() {
        val fakeProcedure = Procedure()
        fakeProcedure.subject = Reference("Patient/fakeFakeFakeId")
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure2.subject = Reference("Patient/fakeFakeFakestId")
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val output =
            procedureProvider.search(
                patient = ReferenceParam("fakeFakeFakeId"),
            )

        assertEquals(1, output.size)
        assertEquals("Procedure/${fakeProcedure.id}", output[0].id)
    }

    @Test
    fun `search by patient using subject`() {
        val fakeProcedure = Procedure()
        fakeProcedure.subject = Reference("Patient/fakeFakeFakeId")
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure2.subject = Reference("fakeFakeFakestId")
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val output =
            procedureProvider.search(
                subject = ReferenceParam("Patient/fakeFakeFakeId"),
            )

        assertEquals(1, output.size)
        assertEquals("Procedure/${fakeProcedure.id}", output[0].id)
    }

    @Test
    fun `search by patient and subject`() {
        val fakeProcedure = Procedure()
        fakeProcedure.subject = Reference("Patient/fakeFakeFakeId")
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure2.subject = Reference("Patient/fakeFakeFakestId")
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val output =
            procedureProvider.search(
                patient = ReferenceParam("fakeFakeFakeId"),
                subject = ReferenceParam("Patient/fakeFakeFakeId"),
            )

        assertEquals(1, output.size)
        assertEquals("Procedure/${fakeProcedure.id}", output[0].id)
    }

    @Test
    fun `search by date range`() {
        collection.remove("true").execute()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now: LocalDate = LocalDate.now()
        val date1 = dateFormat.format(now.minus(Period.ofDays(4)))
        val date2 = dateFormat.format(now.minus(Period.ofDays(8)))
        val date3 = dateFormat.format(now.minus(Period.ofDays(16)))

        val fakeProcedure = Procedure()
        fakeProcedure.setPerformed(DateTimeType("${date1}T00:00:00.000Z"))
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure2.setPerformed(DateTimeType("${date3}T00:00:00.000Z"))
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("ge$date2")
        dateParam.upperBound = DateParam("lt$date1")
        val output =
            procedureProvider.search(
                dateRange = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("Procedure/${fakeProcedure.id}", output[0].id)
    }

    @Test
    fun `search by date range and find multiple`() {
        collection.remove("true").execute()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now: LocalDate = LocalDate.now()
        val date1 = dateFormat.format(now.minus(Period.ofDays(4)))
        val date2 = dateFormat.format(now.minus(Period.ofDays(8)))

        val fakeProcedure = Procedure()
        fakeProcedure.setPerformed(DateTimeType("${date1}T00:00:00.000Z"))
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure2.setPerformed(DateTimeType("${date2}T00:00:00.000Z"))
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("ge$date2")
        dateParam.upperBound = DateParam("lt$date1")
        val output =
            procedureProvider.search(
                dateRange = dateParam,
            )
        assertEquals(2, output.size)
        assertEquals("Procedure/${fakeProcedure.id}", output[0].id)
        assertEquals("Procedure/${fakeProcedure2.id}", output[1].id)
    }

    @Test
    fun `search by date range but nothing found`() {
        collection.remove("true").execute()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val now: LocalDate = LocalDate.now()
        val date1 = dateFormat.format(now.minus(Period.ofDays(4)))

        val fakeProcedure = Procedure()
        fakeProcedure.setPerformed(DateTimeType("${date1}T00:00:00.000Z"))
        fakeProcedure.id = "FAKEPROCEDURE"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure)).execute()

        val fakeProcedure2 = Procedure()
        fakeProcedure.setPerformed(DateTimeType("${date1}T00:00:00.000Z"))
        fakeProcedure2.id = "FAKEPROCEDURE2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeProcedure2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2012-02-22T12:12:00-06:00")
        dateParam.upperBound = DateParam("2016-02-22T12:12:00-06:00")

        val output =
            procedureProvider.search(
                dateRange = dateParam,
            )

        assertEquals(0, output.size)
    }
}
