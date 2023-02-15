package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4EncounterDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4EncounterResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var encounterProvider: R4EncounterResourceProvider
    private lateinit var dao: R4EncounterDAO

    @BeforeAll
    fun beforeTest() {
        collection = createCollection(Encounter::class.simpleName!!)

        val database = mockk<SafeXDev>()
        every { database.createCollection(Encounter::class.java) } returns SafeXDev.SafeCollection(collection)
        dao = R4EncounterDAO(database, FhirContext.forR4())
        encounterProvider = R4EncounterResourceProvider(dao)
    }

    @BeforeEach
    fun clearCollection() {
        collection.remove("true").execute() // clear collection from previous tests
    }

    @Test
    fun `search by patient`() {
        val fakeEncounter = Encounter()
        fakeEncounter.subject = Reference("Patient/fakeFakeFakeId")
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.subject = Reference("Patient/fakeFakeFakestId")
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val output = encounterProvider.search(
            patient = ReferenceParam("fakeFakeFakeId"),
        )

        assertEquals(1, output.size)
        assertEquals("Encounter/${fakeEncounter.id}", output[0].id)
    }

    @Test
    fun `search by patient using subject`() {
        val fakeEncounter = Encounter()
        fakeEncounter.subject = Reference("Patient/fakeFakeFakeId")
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.subject = Reference("fakeFakeFakestId")
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val output = encounterProvider.search(
            subject = ReferenceParam("Patient/fakeFakeFakeId"),
        )

        assertEquals(1, output.size)
        assertEquals("Encounter/${fakeEncounter.id}", output[0].id)
    }

    @Test
    fun `search by patient and subject`() {
        val fakeEncounter = Encounter()
        fakeEncounter.subject = Reference("Patient/fakeFakeFakeId")
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.subject = Reference("Patient/fakeFakeFakestId")
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val output = encounterProvider.search(
            patient = ReferenceParam("fakeFakeFakeId"),
            subject = ReferenceParam("Patient/fakeFakeFakeId"),
        )

        assertEquals(1, output.size)
        assertEquals("Encounter/${fakeEncounter.id}", output[0].id)
    }

    @Test
    fun `search by date range`() {
        val fakeEncounter = Encounter()
        assertEquals(collection.find().execute().count(), 0)

        fakeEncounter.period.start = Date(118, 0, 8)
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.period.start = Date(110, 0, 8)
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2012-02-22T12:12:00-06:00")

        val output = encounterProvider.search(
            dateRange = dateParam
        )

        assertEquals(1, output.size)
        assertEquals("Encounter/${fakeEncounter.id}", output[0].id)
    }

    @Test
    fun `search by date range using lowerbound and upperbound`() {
        val fakeEncounter = Encounter()
        assertEquals(collection.find().execute().count(), 0)

        fakeEncounter.period.start = Date(118, 0, 8)
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.period.start = Date(110, 0, 8)
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2012-02-22T12:12:00-06:00")
        dateParam.upperBound = DateParam("2022-02-22T12:12:00-06:00")

        val output = encounterProvider.search(
            dateRange = dateParam
        )

        assertEquals(1, output.size)
        assertEquals("Encounter/${fakeEncounter.id}", output[0].id)
    }

    @Test
    fun `search by date range using lowerbound and upperbound but nothing found`() {
        val fakeEncounter = Encounter()
        assertEquals(collection.find().execute().count(), 0)

        fakeEncounter.period.start = Date(118, 0, 8)
        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.period.start = Date(110, 0, 8)
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2012-02-22T12:12:00-06:00")
        dateParam.upperBound = DateParam("2016-02-22T12:12:00-06:00")

        val output = encounterProvider.search(
            dateRange = dateParam
        )

        assertEquals(0, output.size)
    }

    @Test
    fun `search by date range but no encounters have start dates`() {
        val fakeEncounter = Encounter()
        assertEquals(collection.find().execute().count(), 0)

        fakeEncounter.id = "FAKEENCOUNTER"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter)).execute()

        val fakeEncounter2 = Encounter()
        fakeEncounter2.id = "FAKEENCOUNTER2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(fakeEncounter2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2012-02-22T12:12:00-06:00")
        dateParam.upperBound = DateParam("2022-02-22T12:12:00-06:00")

        val output = encounterProvider.search(
            dateRange = dateParam
        )

        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource type returned`() {
        assertEquals(encounterProvider.resourceType, Encounter::class.java)
    }

    @Test
    fun `encounter code coverage`() {
        dao.searchByQuery()
    }
}
