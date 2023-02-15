package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
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
        val database = mockk<SafeXDev>()
        every { database.createCollection(DocumentReference::class.java) } returns SafeXDev.SafeCollection(collection)
        val dao = R4DocumentReferenceDAO(database, FhirContext.forR4())
        documentReferenceProvider = R4DocumentReferenceResourceProvider(dao)
    }

    @Test
    fun `patient search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
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
        collection.remove("true").execute() // Clear the collection in case other tests run first
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
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.subject = Reference("Practitioner/pract1")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.id = "TESTINGIDENTIFIER2"

        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val output =
            documentReferenceProvider.search(subjectReferenceParam = ReferenceParam("Practitioner/pract1")).first()
        assertEquals(output.id, "DocumentReference/${testDocumentReference.id}")
    }

    @Test
    fun `null search tests`() {
        val output = documentReferenceProvider.search()
        assertTrue(output.isEmpty())

        val outputNullCategory = documentReferenceProvider.search(
            categoryParam = null
        )
        assertTrue(outputNullCategory.isEmpty())

        val outputNullSubject = documentReferenceProvider.search(
            subjectReferenceParam = null
        )
        assertTrue(outputNullSubject.isEmpty())

        val outputNullPatient = documentReferenceProvider.search(
            patientReferenceParam = null
        )
        assertTrue(outputNullPatient.isEmpty())
    }

    /**
     * BaseResourceDAO provides code that parses an input search string for coded values that may
     * contain any variation of FHIR token format, such as system|code, system|, |code, or code alone.
     * This search string may contain either a single FHIR token or a comma-separated list of FHIR tokens.
     * The same methods inherited from BaseResourceDAO handle FHIR tokens in search queries for any resource.
     * Unit tests for all variations of FHIR tokens as single value vs list are in R4ObservationResourceProvider.
     */
    @Test
    fun `search by category - category(n)_coding(n)_code and category(n)_coding(n)_system or category(n)_text`() {
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

        val tokenCode = TokenParam()
        tokenCode.system = ""
        tokenCode.value = "myCode"
        val tokenListCode = TokenOrListParam()
        tokenListCode.add(tokenCode)
        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"
        val tokenListMixed = TokenOrListParam()
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val outputCode = documentReferenceProvider.search(
            categoryParam = tokenListCode
        )
        assertEquals(4, outputCode.size)
        assertEquals("DocumentReference/${testDocumentReference1.id}", outputCode[0].id)
        assertEquals("DocumentReference/${testDocumentReference3.id}", outputCode[1].id)
        assertEquals("DocumentReference/${testDocumentReference4.id}", outputCode[2].id)
        assertEquals("DocumentReference/${testDocumentReference5.id}", outputCode[3].id)

        val outputSystemCodeList = documentReferenceProvider.search(
            categoryParam = tokenListMixed
        )
        assertEquals(3, outputSystemCodeList.size)
        assertEquals("DocumentReference/${testDocumentReference1.id}", outputSystemCodeList[0].id)
        assertEquals("DocumentReference/${testDocumentReference2.id}", outputSystemCodeList[1].id)
        assertEquals("DocumentReference/${testDocumentReference3.id}", outputSystemCodeList[2].id)
    }

    @Test
    fun `subject AND category search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testDocumentReference1 = DocumentReference()
        testDocumentReference1.id = "TESTINGIDENTIFIER"
        testDocumentReference1.subject = Reference("Practitioner/pract1")
        testDocumentReference1.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference1)).execute()

        val testDocumentReference2 = DocumentReference()
        testDocumentReference2.id = "TESTINGIDENTIFIER2"
        testDocumentReference2.subject = Reference("Practitioner/pract1")
        testDocumentReference2.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference2)).execute()

        val testDocumentReference3 = DocumentReference()
        testDocumentReference3.id = "TESTINGIDENTIFIER3"
        testDocumentReference3.subject = Reference("Practitioner/pract2")
        testDocumentReference3.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference3)).execute()

        val testDocumentReference4 = DocumentReference()
        testDocumentReference4.id = "TESTINGIDENTIFIER4"
        testDocumentReference4.subject = Reference("Practitioner/pract1")
        testDocumentReference4.category = listOf(
            CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference4)).execute()

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"
        val tokenListMine = TokenOrListParam()
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam()
        tokenListOther.add(tokenOther)
        val tokenListMixed = TokenOrListParam()
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val output1 = documentReferenceProvider.search(
            subjectReferenceParam = ReferenceParam("Practitioner/pract1"),
            categoryParam = tokenListMine
        )
        assertEquals(2, output1.size)
        assertEquals("DocumentReference/${testDocumentReference1.id}", output1[0].id)
        assertEquals("DocumentReference/${testDocumentReference2.id}", output1[1].id)

        val output2 = documentReferenceProvider.search(
            subjectReferenceParam = ReferenceParam("Practitioner/pract1"),
            categoryParam = tokenListOther
        )
        assertEquals(1, output2.size)
        assertEquals("DocumentReference/${testDocumentReference4.id}", output2[0].id)

        val output3 = documentReferenceProvider.search(
            subjectReferenceParam = ReferenceParam("Practitioner/pract2"),
            categoryParam = tokenListOther
        )
        assertEquals(0, output3.size)

        val output4 = documentReferenceProvider.search(
            subjectReferenceParam = ReferenceParam("Practitioner/pract2"),
            categoryParam = tokenListMine
        )
        assertEquals(1, output4.size)
        assertEquals("DocumentReference/${testDocumentReference3.id}", output4[0].id)

        val output5 = documentReferenceProvider.search(
            subjectReferenceParam = ReferenceParam("Practitioner/pract1"),
            categoryParam = tokenListMixed
        )
        assertEquals(3, output5.size)
        assertEquals("DocumentReference/${testDocumentReference1.id}", output5[0].id)
        assertEquals("DocumentReference/${testDocumentReference2.id}", output5[1].id)
        assertEquals("DocumentReference/${testDocumentReference4.id}", output5[2].id)
    }

    @Test
    fun `docStatus search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testDocumentReference = DocumentReference()
        testDocumentReference.id = "TESTINGIDENTIFIER"
        testDocumentReference.docStatus = DocumentReference.ReferredDocumentStatus.FINAL
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testDocumentReference)).execute()

        val output =
            documentReferenceProvider.search(docStatusParam = StringParam(DocumentReference.ReferredDocumentStatus.FINAL.toCode()))
        assertEquals("DocumentReference/${testDocumentReference.id}", output[0].id)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(documentReferenceProvider.resourceType, DocumentReference::class.java)
    }
}
