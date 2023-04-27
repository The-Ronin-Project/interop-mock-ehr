package com.projectronin.interop.mock.ehr.stu3.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.stu3.toR4
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev.SafeCollection
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.hl7.fhir.dstu3.model.Appointment
import org.hl7.fhir.dstu3.model.IdType
import org.hl7.fhir.dstu3.model.Reference
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.Date
import java.util.UUID
import org.hl7.fhir.r4.model.Appointment as R4Appointment

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class STU3AppointmentResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var appointmentProvider: STU3AppointmentResourceProvider
    private lateinit var dao: R4AppointmentDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Appointment::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(R4Appointment::class.java) } returns SafeCollection("resource", collection)
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4AppointmentDAO(database, FhirContext.forR4())
        appointmentProvider = STU3AppointmentResourceProvider(dao)
    }

    @Test
    fun `insert test with id`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID1"

        val output = appointmentProvider.create(testAppt)
        assertEquals(output.id, IdType("TESTINGID1"))
    }

    @Test
    fun `insert test without id`() {
        val testAppt = Appointment()

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "UUID-GENERATED-ID"
        val output = appointmentProvider.create(testAppt)
        assertEquals(output.id, IdType("UUID-GENERATED-ID"))
        unmockkStatic(UUID::class)
    }

    @Test
    fun `update test with id`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt.toR4())).execute()

        testAppt.status = Appointment.AppointmentStatus.BOOKED
        val output = appointmentProvider.update(IdType("TESTINGID2"), testAppt)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID2").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4Appointment::class.java, dbDoc.toString())
        assertEquals(outputAppt.status, R4Appointment.AppointmentStatus.BOOKED)
    }

    @Test
    fun `update test without id`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt.toR4())).execute()
        testAppt.status = Appointment.AppointmentStatus.BOOKED

        val output = appointmentProvider.updateNoId(testAppt)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID3").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4Appointment::class.java, dbDoc.toString())
        assertEquals(outputAppt.status, R4Appointment.AppointmentStatus.BOOKED)
    }

    @Test
    fun `update test not found so create new`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID4"
        val output = appointmentProvider.updateNoId(testAppt)
        assertTrue(output.created)

        val dbDoc = collection.find("id = :id").bind("id", "TESTINGID4").execute().fetchOne()
        val outputAppt = FhirContext.forR4().newJsonParser().parseResource(R4Appointment::class.java, dbDoc.toString())
        assertNotNull(outputAppt.id)
    }

    @Test
    fun `delete test`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt.toR4())).execute()

        appointmentProvider.delete(IdType("TESTINGID5"))
        Assertions.assertNull(collection.find("id = :id").bind("id", "TESTINGID5").execute().fetchOne())
    }

    @Test
    fun `read test`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt.toR4())).execute()

        val output = appointmentProvider.read(IdType("TESTINGID6"))
        assertEquals(output.id, "Appointment/${testAppt.id}")
    }

    @Test
    fun `read test all`() {
        val testAppt = Appointment()
        testAppt.id = "TESTINGID7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt.toR4())).execute()
        val testAppt2 = Appointment()
        testAppt.id = "TESTINGID8"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testAppt2.toR4())).execute()
        val output = appointmentProvider.returnAll()
        assertTrue(output.size > 1)
    }

    @Test
    fun `search by patient`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID1")
        testAppt.id = "TESTAPPT1"
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Patient/BADID")
        testAppt2.id = "TESTAPPT2"
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output = appointmentProvider.search(patientReferenceParam = ReferenceParam("TESTINGID1"))
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)
    }

    @Test
    fun `search by csn`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID9")
        testAppt.id = "TESTAPPT9"
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt)).execute()
        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Patient/TESTINGID10")
        testAppt2.id = "TESTAPPT10"
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output = appointmentProvider.search(
            patientReferenceParam = ReferenceParam("TESTINGID9"),
            identifiersParam = TokenOrListParam("mockEncounterCSNSystem", "TESTAPPT9")
        )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)

        val output2 = appointmentProvider.search(
            patientReferenceParam = ReferenceParam("TESTINGID9"),
            identifiersParam = TokenOrListParam("mockEncounterCSNSystem", "TESTAPPT9", "TESTAPPT10")
        )
        assertEquals(2, output2.size)

        assertThrows<UnsupportedOperationException> {
            appointmentProvider.search(
                patientReferenceParam = ReferenceParam(
                    "TESTINGID9"
                ),
                identifiersParam = TokenOrListParam("badSystem", "12345")
            )
        }

        assertEquals(
            emptyList<Appointment>(),
            appointmentProvider.search(
                patientReferenceParam = ReferenceParam(
                    "TESTINGID9"
                ),
                identifiersParam = TokenOrListParam()
            )
        )
    }

    @Test
    fun `search by status`() {
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID4")
        testAppt.id = "TESTAPPT1"
        testAppt.status = Appointment.AppointmentStatus.BOOKED
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.addParticipant().actor = Reference("Patient/TESTINGID4")
        testAppt2.id = "TESTAPPT2"
        testAppt2.status = Appointment.AppointmentStatus.CANCELLED
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val output = appointmentProvider.search(
            patientReferenceParam = ReferenceParam("TESTINGID4"),
            statusParam = StringParam("booked")
        )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt.id}", output[0].id)
    }

    @Test
    fun `search by date range`() {
        val testAppt = Appointment()
        testAppt.start = Date(110, 0, 10)
        testAppt.id = "TESTAPPT5"
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID2")
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.start = Date(115, 0, 10)
        testAppt2.id = "TESTAPPT6"
        testAppt2.addParticipant().actor = Reference("Patient/TESTINGID2")
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")
        val output = appointmentProvider.search(
            dateRangeParam = dateParam,
            patientReferenceParam = ReferenceParam("TESTINGID2")
        )
        assertEquals(1, output.size)
        assertEquals("Appointment/${testAppt2.id}", output[0].id)
    }

    @Test
    fun `search by date range but no appointments have start times`() {
        collection.remove("true").execute() // clears the collection if other tests run first
        assertEquals(collection.find().execute().count(), 0)
        val testAppt = Appointment()
        testAppt.addParticipant().actor = Reference("Patient/TESTINGID3")
        testAppt.id = "TESTAPPT7"
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt)).execute()

        val testAppt2 = Appointment()
        testAppt2.id = "TESTAPPT8"
        testAppt2.addParticipant().actor = Reference("Patient/TESTINGID3")
        collection.add(FhirContext.forDstu3().newJsonParser().encodeResourceToString(testAppt2)).execute()

        val dateParam = DateRangeParam()
        dateParam.lowerBound = DateParam("2011-02-22T13:12:00-06:00")
        dateParam.upperBound = DateParam("2020-02-22T13:12:00-06:00")
        val output = appointmentProvider.search(
            dateRangeParam = dateParam,
            patientReferenceParam = ReferenceParam("TESTINGID3")
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
