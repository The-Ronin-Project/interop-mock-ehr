package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ObservationDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4ObservationResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var observationProvider: R4ObservationResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(Observation::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Observation::class.simpleName, true) } returns collection
        observationProvider = R4ObservationResourceProvider(R4ObservationDAO(database))
    }

    @Test
    fun `patient search test`() {
        val testObservation = Observation()
        testObservation.id = "TESTINGIDENTIFIER"
        testObservation.status = Observation.ObservationStatus.FINAL
        testObservation.subject = Reference("Patient/patID")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.status = Observation.ObservationStatus.AMENDED

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val output = observationProvider.search(patientReferenceParam = ReferenceParam("patID")).first()
        assertEquals(output.status, testObservation.status)
    }

    @Test
    fun `null search test`() {
        val output = observationProvider.search()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search by category - category(n)_coding(n)_code or category(n)_text`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testObservation1 = Observation()
        testObservation1.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testObservation1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.category = listOf(CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay")))
        testObservation2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testObservation3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "myCode"
        testObservation4.category = listOf(codeableConcept4)
        testObservation4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val testObservation5 = Observation()
        val codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "otherCode"
        testObservation5.category = listOf(codeableConcept5, codeableConcept4)
        testObservation5.id = "${prefix}TESTCOND5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation5)).execute()

        val testObservation6 = Observation()
        testObservation6.category = listOf(codeableConcept5)
        testObservation6.id = "${prefix}TESTCOND6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation6)).execute()

        val testObservation7 = Observation()
        testObservation7.category = listOf()
        testObservation7.id = "${prefix}TESTCOND7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation7)).execute()

        val output = observationProvider.search(categoryParam = TokenParam("myCode"))
        assertEquals(4, output.size)
        assertEquals("Observation/${testObservation1.id}", output[0].id)
        assertEquals("Observation/${testObservation3.id}", output[1].id)
        assertEquals("Observation/${testObservation4.id}", output[2].id)
        assertEquals("Observation/${testObservation5.id}", output[3].id)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(observationProvider.resourceType, Observation::class.java)
    }
}
