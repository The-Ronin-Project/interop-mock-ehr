package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4DocumentReferenceResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var documentReferenceProvider: R4DocumentReferenceResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(DocumentReference::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(DocumentReference::class.simpleName, true) } returns collection
        documentReferenceProvider = R4DocumentReferenceResourceProvider(R4DocumentReferenceDAO(database))
    }

    @Test
    fun `patient search test`() {
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.subject = Reference("Patient/patID")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.id = "TESTINGIDENTIFIER2"

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val output = documentReferenceProvider.search(patientReferenceParam = ReferenceParam("patID")).first()
        assertEquals(output.id, "DocumentReference/${testDocumentReference.id}")
    }

    @Test
    fun `encounter search test`() {
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.context.encounter = listOf(Reference("Encounter/encounter"))
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.id = "TESTINGIDENTIFIER2"

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val output = documentReferenceProvider.search(encounterReferenceParam = ReferenceParam("encounter")).first()
        assertEquals(output.id, "DocumentReference/${testDocumentReference.id}")
    }

    @Test
    fun `subject search test`() {
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.subject = Reference("Practitioner/pract1")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.id = "TESTINGIDENTIFIER2"

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val output = documentReferenceProvider.search(subjectReferenceParam = ReferenceParam("Practitioner/pract1")).first()
        assertEquals(output.id, "DocumentReference/${testDocumentReference.id}")
    }

    @Test
    fun `null search test`() {
        val output = documentReferenceProvider.search()
        assertTrue(output.isEmpty())
    }

    @Test
    fun `search by category - category(n)_coding(n)_code or category(n)_text`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testDocumentReference1 = DocumentReference()
        testDocumentReference1.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testDocumentReference1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference1)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.category = listOf(CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay")))
        testDocumentReference2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val testDocumentReference3 = DocumentReference()
        testDocumentReference3.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testDocumentReference3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference3)).execute()

        val testDocumentReference4 = DocumentReference()
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "myCode"
        testDocumentReference4.category = listOf(codeableConcept4)
        testDocumentReference4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference4)).execute()

        val testDocumentReference5 = DocumentReference()
        val codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "otherCode"
        testDocumentReference5.category = listOf(codeableConcept5, codeableConcept4)
        testDocumentReference5.id = "${prefix}TESTCOND5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference5)).execute()

        val testDocumentReference6 = DocumentReference()
        testDocumentReference6.category = listOf(codeableConcept5)
        testDocumentReference6.id = "${prefix}TESTCOND6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference6)).execute()

        val testDocumentReference7 = DocumentReference()
        testDocumentReference7.category = listOf()
        testDocumentReference7.id = "${prefix}TESTCOND7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference7)).execute()

        val output = documentReferenceProvider.search(categoryParam = TokenParam("myCode"))
        assertEquals(4, output.size)
        assertEquals("DocumentReference/${testDocumentReference1.id}", output[0].id)
        assertEquals("DocumentReference/${testDocumentReference3.id}", output[1].id)
        assertEquals("DocumentReference/${testDocumentReference4.id}", output[2].id)
        assertEquals("DocumentReference/${testDocumentReference5.id}", output[3].id)
    }

    @Test
    fun `docStatus search test`() {
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.docStatus = DocumentReference.ReferredDocumentStatus.FINAL
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val output = documentReferenceProvider.search(docStatusParam = StringParam(DocumentReference.ReferredDocumentStatus.FINAL.toCode()))
        assertEquals("DocumentReference/${testDocumentReference.id}", output[0].id)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(documentReferenceProvider.resourceType, DocumentReference::class.java)
    }
}
