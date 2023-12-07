package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4AppointmentResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var appointmentProvider: R4AppointmentResourceProvider
    private lateinit var dao: R4AppointmentDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Appointment::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Appointment::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4AppointmentDAO(database, FhirContext.forR4())
        appointmentProvider = R4AppointmentResourceProvider(dao)
    }

    @Test
    fun `search by patient`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID")
        testAppt.id = "TESTAPPT1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Patient/BADID")
        testAppt2.id = "TESTAPPT2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output = appointmentProvider.search(patientReferenceParam = ReferenceParam("TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)
    }

    @Test
    fun `search by practitioner and generic`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Practitioner/IPMD")
        testAppt.addParticipant().actor = Reference("Location/1")
        testAppt.id = "TESTAPPT3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Practitioner/BADID")
        testAppt2.addParticipant().actor = Reference("Location/2")
        testAppt2.id = "TESTAPPT4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output =
            appointmentProvider.search(
                practitionerReferenceParam = ReferenceParam("IPMD"),
                referenceParam = ReferenceParam("Location/1"),
            )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)
    }

    @Test
    fun `search by location`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Practitioner/IPMD")
        testAppt.addParticipant().actor = Reference("Location/LocationFHIRID1")
        testAppt.id = "TESTAPPT9"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Practitioner/BADID")
        testAppt2.addParticipant().actor = Reference("Location/BADID")
        testAppt2.id = "TESTAPPT10"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output =
            appointmentProvider.search(
                practitionerReferenceParam = ReferenceParam("IPMD"),
                locationParam = ReferenceParam("LocationFHIRID1"),
            )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)
    }

    @Test
    fun `search by date range`() {
        val testAppt = Appointment()
        testAppt.start = Date(110, 0, 10)
        testAppt.id = "TESTAPPT5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.start = Date(115, 0, 10)
        testAppt2.id = "TESTAPPT6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")
        val output =
            appointmentProvider.search(
                dateRangeParam = dateParam,
            )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt2.id}", output[0].id)
    }

    @Test
    fun `search by date range but no appointments have start times`() {
        collection.remove("true").execute() // clears the collection if other tests run first
        assertEquals(collection.find().execute().count(), 0)
        val testAppt = Appointment()
        testAppt.id = "TESTAPPT7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.id = "TESTAPPT8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")
        dateParam.upperBound = DateParam("2020-02-22T13:12:00-06:00")
        val output =
            appointmentProvider.search(
                dateRangeParam = dateParam,
            )
        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(appointmentProvider.resourceType, Appointment::class.java)
    }

    @Test
    fun `appointment code coverage test!`() {
        dao.searchByQuery() // tests default parameter values
    }
}
