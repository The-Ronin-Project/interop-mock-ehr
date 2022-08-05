package com.projectronin.interop.mock.ehr.stu3.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.stu3.dao.STU3AppointmentDAO
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.dstu3.model.Appointment
import org.hl7.fhir.dstu3.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class STU3AppointmentResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var appointmentProvider: STU3AppointmentResourceProvider
    private lateinit var dao: STU3AppointmentDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Appointment::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Appointment::class.simpleName, true) } returns collection
        dao = STU3AppointmentDAO(database)
        appointmentProvider = STU3AppointmentResourceProvider(dao)
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
