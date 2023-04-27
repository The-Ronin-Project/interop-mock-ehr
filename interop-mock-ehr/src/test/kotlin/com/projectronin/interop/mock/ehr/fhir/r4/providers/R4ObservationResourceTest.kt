package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ObservationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Date
import org.hl7.fhir.r4.model.Period as R4Period

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4ObservationResourceTest : BaseMySQLTest() {

    private lateinit var collection: Collection
    private lateinit var observationProvider: R4ObservationResourceProvider

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val now: LocalDate = LocalDate.now()
    private val dateString3 = dateFormat.format(now.minus(Period.ofDays(3))) // 3 days ago
    private val dateString4 = dateFormat.format(now.minus(Period.ofDays(4)))
    private val dateString5 = dateFormat.format(now.minus(Period.ofDays(5)))
    private val dateString6 = dateFormat.format(now.minus(Period.ofDays(6)))
    private val dateString7 = dateFormat.format(now.minus(Period.ofDays(7)))
    private val dateString8 = dateFormat.format(now.minus(Period.ofDays(8)))

    @BeforeAll
    fun initTest() {
        collection = createCollection(Observation::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Observation::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        val dao = R4ObservationDAO(database, FhirContext.forR4())
        observationProvider = R4ObservationResourceProvider(dao)
    }

    @Test
    fun `subject search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.status = Observation.ObservationStatus.FINAL
        testObservation1.subject = Reference("Patient/patID")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.status = Observation.ObservationStatus.AMENDED
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val output = observationProvider.search(subjectReferenceParam = ReferenceParam("Patient/patID"))
        assertEquals(1, output.size)
        assertEquals(testObservation1.status, output[0].status)
    }

    @Test
    fun `patient search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.status = Observation.ObservationStatus.FINAL
        testObservation1.subject = Reference("Patient/patID")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.status = Observation.ObservationStatus.AMENDED
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val output = observationProvider.search(patientReferenceParam = ReferenceParam("patID")).first()
        assertEquals(output.status, testObservation1.status)
    }

    @Test
    fun `patient (preferred to subject) search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.status = Observation.ObservationStatus.FINAL
        testObservation1.subject = Reference("Patient/patID")
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.status = Observation.ObservationStatus.AMENDED
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val output = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/otherID"),
            patientReferenceParam = ReferenceParam("patID")
        ).first()
        assertEquals(output.status, testObservation1.status)

        val outputNotFound = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patID"),
            patientReferenceParam = ReferenceParam("otherID")
        )
        assertTrue(outputNotFound.isEmpty())
    }

    @Test
    fun `null search tests`() {
        val output = observationProvider.search()
        assertTrue(output.isEmpty())

        val outputNullCategory = observationProvider.search(
            categoryParam = null
        )
        assertTrue(outputNullCategory.isEmpty())

        val outputNullSubject = observationProvider.search(
            subjectReferenceParam = null
        )
        assertTrue(outputNullSubject.isEmpty())

        val outputNullPatient = observationProvider.search(
            patientReferenceParam = null
        )
        assertTrue(outputNullPatient.isEmpty())
    }

    /**
     * BaseResourceDAO provides code that parses an input search string for coded values that may
     * contain any variation of FHIR token format, such as system|code, system|, |code, or code alone.
     * This search string may contain either a single FHIR token or a comma-separated list of FHIR tokens.
     * The same methods inherited from BaseResourceDAO handle FHIR tokens in search queries for any resource.
     * Unit tests for all variations of FHIR tokens as single value vs list are here, in R4ObservationResourceProvider.
     */
    @Test
    fun `search by category - code - category(n)_coding(n)_code or category(n)_text`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testObservation1 = Observation()
        testObservation1.category = listOf(
            CodeableConcept(
                Coding("mySystem", "myCode", "myDisplay")
            )
        )
        testObservation1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.category = listOf(
            CodeableConcept(
                Coding("otherSystem", "otherCode", "otherDisplay")
            )
        )
        testObservation2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.category = listOf(
            CodeableConcept(
                Coding("mySystem", "myCode", "myDisplay")
            )
        )
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
        val tokenListMine = TokenOrListParam()
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam()
        tokenListOther.add(tokenOther)
        val tokenListMixed = TokenOrListParam()
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val outputMyCode = observationProvider.search(
            categoryParam = tokenListCode
        )
        assertEquals(4, outputMyCode.size)
        assertEquals("Observation/${testObservation1.id}", outputMyCode[0].id)
        assertEquals("Observation/${testObservation3.id}", outputMyCode[1].id)
        assertEquals("Observation/${testObservation4.id}", outputMyCode[2].id)
        assertEquals("Observation/${testObservation5.id}", outputMyCode[3].id)

        val outputOtherCode = observationProvider.search(
            categoryParam = tokenListOther
        )
        assertEquals(1, outputOtherCode.size)
        assertEquals("Observation/${testObservation2.id}", outputOtherCode[0].id)

        val outputCode = observationProvider.search(
            categoryParam = tokenListMixed
        )
        assertEquals(3, outputCode.size)
        assertEquals("Observation/${testObservation1.id}", outputCode[0].id)
        assertEquals("Observation/${testObservation2.id}", outputCode[1].id)
        assertEquals("Observation/${testObservation3.id}", outputCode[2].id)
    }

    @Test
    fun `search by category - system-pipe-code - category(n)_coding(n)_code and category(n)_coding(n)_system`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testObservation1 = Observation()
        testObservation1.category = listOf(
            CodeableConcept(
                Coding("mySystem", "myCode", "myDisplay")
            )
        )
        testObservation1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.category = listOf(
            CodeableConcept(
                Coding("otherSystem", "otherCode", "otherDisplay")
            )
        )
        testObservation2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.category = listOf(
            CodeableConcept(
                Coding("mySystem", "myCode", "myDisplay")
            )
        )
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

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"

        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam() // "otherSystem|otherCode"
        tokenListOther.add(tokenOther)
        val tokenListMixed = TokenOrListParam() // "mySystem|myCode,otherSystem|otherCode"
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val outputMyCode = observationProvider.search(
            categoryParam = tokenListMine
        )
        assertEquals(2, outputMyCode.size)
        assertEquals("Observation/${testObservation1.id}", outputMyCode[0].id)
        assertEquals("Observation/${testObservation3.id}", outputMyCode[1].id)

        val outputOtherCode = observationProvider.search(
            categoryParam = tokenListOther
        )
        assertEquals(1, outputOtherCode.size)
        assertEquals("Observation/${testObservation2.id}", outputOtherCode[0].id)

        val outputCode = observationProvider.search(
            categoryParam = tokenListMixed
        )
        assertEquals(3, outputCode.size)
        assertEquals("Observation/${testObservation1.id}", outputCode[0].id)
        assertEquals("Observation/${testObservation2.id}", outputCode[1].id)
        assertEquals("Observation/${testObservation3.id}", outputCode[2].id)
    }

    @Test
    fun `search by category - system-pipe - category(n)_coding(n)_system`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testObservation1 = Observation()
        testObservation1.category = listOf(
            CodeableConcept(Coding("mySystem", "A", "myDisplay")),
            CodeableConcept(Coding("otherSystem", "A", "myDisplay"))
        )
        testObservation1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.category = listOf(
            CodeableConcept(Coding("otherSystem", "A", "otherDisplay")),
            CodeableConcept(Coding("theirSystem", "A", "myDisplay"))
        )
        testObservation2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.category = listOf(
            CodeableConcept(Coding("theirSystem", "A", "myDisplay"))
        )
        testObservation3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "A"
        testObservation4.category = listOf(codeableConcept4)
        testObservation4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val testObservation5 = Observation()
        val codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "A"
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

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = ""
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = ""
        val tokenTheir = TokenParam()
        tokenTheir.system = "theirSystem"
        tokenTheir.value = ""
        val tokenA = TokenParam()
        tokenA.system = ""
        tokenA.value = "A"

        val tokenListMine = TokenOrListParam() // "mySystem|"
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam() // "otherSystem|"
        tokenListOther.add(tokenOther)
        val tokenListTheir = TokenOrListParam() // "theirSystem|"
        tokenListTheir.add(tokenTheir)
        val tokenListA = TokenOrListParam() // "A"
        tokenListA.add(tokenA)
        val tokenListMixed1 = TokenOrListParam() // "theirSystem|,mySystem|"
        tokenListMixed1.add(tokenTheir)
        tokenListMixed1.add(tokenMine)
        val tokenListMixed2 = TokenOrListParam() // "otherSystem|,mySystem|"
        tokenListMixed2.add(tokenOther)
        tokenListMixed2.add(tokenMine)

        // code filtered by system
        val outputMySystem = observationProvider.search(
            categoryParam = tokenListMine
        )
        assertEquals(1, outputMySystem.size)
        assertEquals("Observation/${testObservation1.id}", outputMySystem[0].id)

        val outputOtherSystem = observationProvider.search(
            categoryParam = tokenListOther
        )
        assertEquals(2, outputOtherSystem.size)
        assertEquals("Observation/${testObservation1.id}", outputOtherSystem[0].id)
        assertEquals("Observation/${testObservation2.id}", outputOtherSystem[1].id)

        val outputTheirSystem = observationProvider.search(
            categoryParam = tokenListTheir
        )
        assertEquals(2, outputTheirSystem.size)
        assertEquals("Observation/${testObservation2.id}", outputTheirSystem[0].id)
        assertEquals("Observation/${testObservation3.id}", outputTheirSystem[1].id)

        // code not filtered by system
        val outputCode = observationProvider.search(
            categoryParam = tokenListA
        )
        assertEquals(6, outputCode.size)
        assertEquals("Observation/${testObservation1.id}", outputCode[0].id)
        assertEquals("Observation/${testObservation2.id}", outputCode[1].id)
        assertEquals("Observation/${testObservation3.id}", outputCode[2].id)
        assertEquals("Observation/${testObservation4.id}", outputCode[3].id)
        assertEquals("Observation/${testObservation5.id}", outputCode[4].id)
        assertEquals("Observation/${testObservation6.id}", outputCode[5].id)

        // code filtered by system in a list
        val outputListSystem1 = observationProvider.search(
            categoryParam = tokenListMixed1
        )
        assertEquals(3, outputListSystem1.size)
        assertEquals("Observation/${testObservation1.id}", outputListSystem1[0].id)
        assertEquals("Observation/${testObservation2.id}", outputListSystem1[1].id)
        assertEquals("Observation/${testObservation3.id}", outputListSystem1[2].id)

        val outputListSystem2 = observationProvider.search(
            categoryParam = tokenListMixed2
        )
        assertEquals(2, outputListSystem2.size)
        assertEquals("Observation/${testObservation1.id}", outputListSystem2[0].id)
        assertEquals("Observation/${testObservation2.id}", outputListSystem2[1].id)
    }

    @Test
    fun `search by category - mixed token formats in lists`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testObservation1 = Observation()
        testObservation1.category = listOf(
            CodeableConcept(Coding("mySystem", "A", "myDisplay")),
            CodeableConcept(Coding("otherSystem", "B", "myDisplay"))
        )
        testObservation1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.category = listOf(
            CodeableConcept(Coding("otherSystem", "A", "otherDisplay")),
            CodeableConcept(Coding("theirSystem", "A", "myDisplay"))
        )
        testObservation2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.category = listOf(
            CodeableConcept(Coding("mySystem", "A", "myDisplay")),
            CodeableConcept(Coding("theirSystem", "B", "myDisplay"))
        )
        testObservation3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "A"
        testObservation4.category = listOf(codeableConcept4)
        testObservation4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val testObservation5 = Observation()
        val codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "B"
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

        val tokenMineA = TokenParam()
        tokenMineA.system = "mySystem"
        tokenMineA.value = "A"
        val tokenMineB = TokenParam()
        tokenMineB.system = "mySystem"
        tokenMineB.value = "B"
        val tokenOtherA = TokenParam()
        tokenOtherA.system = "otherSystem"
        tokenOtherA.value = "A"
        val tokenOtherB = TokenParam()
        tokenOtherB.system = "otherSystem"
        tokenOtherB.value = "B"
        val tokenTheir = TokenParam()
        tokenTheir.system = "theirSystem"
        tokenTheir.value = ""
        val tokenTheirA = TokenParam()
        tokenTheirA.system = "theirSystem"
        tokenTheirA.value = "A"
        val tokenTheirB = TokenParam()
        tokenTheirB.system = "theirSystem"
        tokenTheirB.value = "B"
        val tokenA = TokenParam()
        tokenA.system = ""
        tokenA.value = "A"
        val tokenB = TokenParam()
        tokenB.system = ""
        tokenB.value = "B"

        val tokenList1 = TokenOrListParam() // "B,otherSystem|A"
        tokenList1.add(tokenB)
        tokenList1.add(tokenOtherA)
        val tokenList2 = TokenOrListParam() // "A,otherSystem|B"
        tokenList2.add(tokenA)
        tokenList2.add(tokenOtherB)
        val tokenList3 = TokenOrListParam() // "theirSystem|,mySystem|B,otherSystem|B"
        tokenList3.add(tokenTheir)
        tokenList3.add(tokenMineB)
        tokenList3.add(tokenOtherB)

        val outputMixedList1 = observationProvider.search(
            categoryParam = tokenList1
        )
        assertEquals(5, outputMixedList1.size)
        assertEquals("Observation/${testObservation1.id}", outputMixedList1[0].id)
        assertEquals("Observation/${testObservation2.id}", outputMixedList1[1].id)
        assertEquals("Observation/${testObservation3.id}", outputMixedList1[2].id)
        assertEquals("Observation/${testObservation5.id}", outputMixedList1[3].id)
        assertEquals("Observation/${testObservation6.id}", outputMixedList1[4].id)

        val outputMixedList2 = observationProvider.search(
            categoryParam = tokenList2
        )
        assertEquals(5, outputMixedList2.size)
        assertEquals("Observation/${testObservation1.id}", outputMixedList2[0].id)
        assertEquals("Observation/${testObservation2.id}", outputMixedList2[1].id)
        assertEquals("Observation/${testObservation3.id}", outputMixedList2[2].id)
        assertEquals("Observation/${testObservation4.id}", outputMixedList2[3].id)
        assertEquals("Observation/${testObservation5.id}", outputMixedList2[4].id)

        val outputMixedList3 = observationProvider.search(
            categoryParam = tokenList3
        )
        assertEquals(3, outputMixedList3.size)
        assertEquals("Observation/${testObservation1.id}", outputMixedList3[0].id)
        assertEquals("Observation/${testObservation2.id}", outputMixedList3[1].id)
        assertEquals("Observation/${testObservation3.id}", outputMixedList3[2].id)
    }

    @Test
    fun `subject AND category search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.subject = Reference("Patient/patient1")
        testObservation1.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.subject = Reference("Patient/patient1")
        testObservation2.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.id = "TESTINGIDENTIFIER3"
        testObservation3.subject = Reference("Patient/patient2")
        testObservation3.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        testObservation4.id = "TESTINGIDENTIFIER4"
        testObservation4.subject = Reference("Patient/patient1")
        testObservation4.category = listOf(
            CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"

        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam() // "otherSystem|otherCode"
        tokenListOther.add(tokenOther)
        val tokenListMixed = TokenOrListParam() // "mySystem|myCode,otherSystem|otherCode"
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val output1 = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient1"),
            categoryParam = tokenListMine
        )
        assertEquals(2, output1.size)
        assertEquals("Observation/${testObservation1.id}", output1[0].id)
        assertEquals("Observation/${testObservation2.id}", output1[1].id)

        val output2 = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient1"),
            categoryParam = tokenListOther
        )
        assertEquals(1, output2.size)
        assertEquals("Observation/${testObservation4.id}", output2[0].id)

        val output3 = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient2"),
            categoryParam = tokenListOther
        )
        assertEquals(0, output3.size)

        val output4 = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient2"),
            categoryParam = tokenListMine
        )
        assertEquals(1, output4.size)
        assertEquals("Observation/${testObservation3.id}", output4[0].id)

        val output5 = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient1"),
            categoryParam = tokenListMixed
        )
        assertEquals(3, output5.size)
        assertEquals("Observation/${testObservation1.id}", output5[0].id)
        assertEquals("Observation/${testObservation2.id}", output5[1].id)
        assertEquals("Observation/${testObservation4.id}", output5[2].id)
    }

    @Test
    fun `patient AND category search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.subject = Reference("Patient/patient1")
        testObservation1.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.subject = Reference("Patient/patient1")
        testObservation2.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.id = "TESTINGIDENTIFIER3"
        testObservation3.subject = Reference("Patient/patient2")
        testObservation3.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        testObservation4.id = "TESTINGIDENTIFIER4"
        testObservation4.subject = Reference("Patient/patient1")
        testObservation4.category = listOf(
            CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"

        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)
        val tokenListOther = TokenOrListParam() // "otherSystem|otherCode"
        tokenListOther.add(tokenOther)
        val tokenListMixed = TokenOrListParam() // "mySystem|myCode,otherSystem|otherCode"
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val output1a = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine
        )
        assertEquals(2, output1a.size)
        assertEquals("Observation/${testObservation1.id}", output1a[0].id)
        assertEquals("Observation/${testObservation2.id}", output1a[1].id)

        val output2a = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListOther
        )
        assertEquals(1, output2a.size)
        assertEquals("Observation/${testObservation4.id}", output2a[0].id)

        val output3a = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient2"),
            categoryParam = tokenListOther
        )
        assertEquals(0, output3a.size)

        val output4a = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient2"),
            categoryParam = tokenListMine
        )
        assertEquals(1, output4a.size)
        assertEquals("Observation/${testObservation3.id}", output4a[0].id)

        val output5a = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMixed
        )
        assertEquals(3, output5a.size)
        assertEquals("Observation/${testObservation1.id}", output5a[0].id)
        assertEquals("Observation/${testObservation2.id}", output5a[1].id)
        assertEquals("Observation/${testObservation4.id}", output5a[2].id)
    }

    @Test
    fun `patient (preferred to subect) AND category search test`() {
        collection.remove("true").execute() // Clear the collection in case other tests run first
        val testObservation1 = Observation()
        testObservation1.id = "TESTINGIDENTIFIER"
        testObservation1.subject = Reference("Patient/patient1")
        testObservation1.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation1)).execute()

        val testObservation2 = Observation()
        testObservation2.id = "TESTINGIDENTIFIER2"
        testObservation2.subject = Reference("Patient/patient1")
        testObservation2.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation2)).execute()

        val testObservation3 = Observation()
        testObservation3.id = "TESTINGIDENTIFIER3"
        testObservation3.subject = Reference("Patient/patient2")
        testObservation3.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation3)).execute()

        val testObservation4 = Observation()
        testObservation4.id = "TESTINGIDENTIFIER4"
        testObservation4.subject = Reference("Patient/patient1")
        testObservation4.category = listOf(
            CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        )
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testObservation4)).execute()

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenOther = TokenParam()
        tokenOther.system = "otherSystem"
        tokenOther.value = "otherCode"

        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)
        val tokenListMixed = TokenOrListParam() // "mySystem|myCode,otherSystem|otherCode"
        tokenListMixed.add(tokenMine)
        tokenListMixed.add(tokenOther)

        val output1b = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient2"),
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine
        )
        assertEquals(2, output1b.size)
        assertEquals("Observation/${testObservation1.id}", output1b[0].id)
        assertEquals("Observation/${testObservation2.id}", output1b[1].id)

        val output4b = observationProvider.search(
            subjectReferenceParam = ReferenceParam("Patient/patient1"),
            patientReferenceParam = ReferenceParam("patient2"),
            categoryParam = tokenListMixed
        )
        assertEquals(1, output4b.size)
        assertEquals("Observation/${testObservation3.id}", output4b[0].id)
    }

    @Test
    fun `search by date range - date time`() {
        val prefix = "date-"
        collection.remove("true").execute() // Clear the collection in case other tests run first

        // 5 days ago
        val observation5 = Observation()
        observation5.id = "${prefix}TESTCOND5"
        observation5.subject = Reference("Patient/patient1")
        observation5.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        observation5.setEffective(DateTimeType("${dateString5}T00:00:00.000Z"))
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation5)).execute()

        // 7 days ago
        val observation7 = Observation()
        observation7.id = "${prefix}TESTCOND7"
        observation7.subject = Reference("Patient/patient1")
        observation7.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        observation7.setEffective(DateTimeType("${dateString7}T00:00:00.000Z"))
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation7)).execute()
        assertEquals(2, collection.count())

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)

        // at limit: start
        val dateParam7to4 = DateRangeParam()
        dateParam7to4.lowerBound = DateParam("ge$dateString7")
        dateParam7to4.upperBound = DateParam("lt$dateString4")
        val output7to4 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam7to4
        )
        assertEquals(2, output7to4.size)
        assertEquals("Observation/${observation5.id}", output7to4[0].id)
        assertEquals("Observation/${observation7.id}", output7to4[1].id)

        // include one, exclude one: start
        val dateParam6to4 = DateRangeParam()
        dateParam6to4.lowerBound = DateParam("ge$dateString6")
        dateParam6to4.upperBound = DateParam("lt$dateString4")
        val output6to4 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam6to4
        )
        assertEquals(1, output6to4.size)
        assertEquals("Observation/${observation5.id}", output6to4[0].id)

        // exclude both: start
        val dateParam4toNow = DateRangeParam()
        dateParam4toNow.lowerBound = DateParam("ge$dateString4")
        val output4toNow = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam4toNow
        )
        assertEquals(0, output4toNow.size)

        // at limit: end
        val dateParam8to5 = DateRangeParam()
        dateParam8to5.lowerBound = DateParam("ge$dateString8")
        dateParam8to5.upperBound = DateParam("lt$dateString5")
        val output8to5 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8to5
        )
        assertEquals(2, output8to5.size)
        assertEquals("Observation/${observation5.id}", output8to5[0].id)
        assertEquals("Observation/${observation7.id}", output8to5[1].id)

        // include one, exclude one: end
        val dateParam8to6 = DateRangeParam()
        dateParam8to6.lowerBound = DateParam("ge$dateString8")
        dateParam8to6.upperBound = DateParam("lt$dateString6")
        val output8to6 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8to6
        )
        assertEquals(1, output8to6.size)
        assertEquals("Observation/${observation7.id}", output8to6[0].id)

        // exclude both: end
        val dateParam8toNow = DateRangeParam()
        dateParam8toNow.upperBound = DateParam("lt$dateString8")
        val output8toNow = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8toNow
        )
        assertEquals(0, output8toNow.size)
    }

    @Test
    fun `search by date range - period`() {
        val prefix = "per-"
        collection.remove("true").execute() // Clear the collection in case other tests run first

        // 5 days ago
        val observation5to4 = Observation()
        observation5to4.id = "${prefix}TESTCOND5TO4"
        observation5to4.subject = Reference("Patient/patient1")
        observation5to4.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        val period5to4 = R4Period()
        period5to4.setStartElement(DateTimeType("${dateString5}T00:00:00.000Z"))
        period5to4.setEndElement(DateTimeType("${dateString4}T00:00:00.000Z"))
        observation5to4.effective = period5to4
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation5to4)).execute()

        // 7 days ago
        val observation7to4 = Observation()
        observation7to4.id = "${prefix}TESTCOND7TO4"
        observation7to4.subject = Reference("Patient/patient1")
        observation7to4.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        val period7to4 = R4Period()
        period7to4.setStartElement(DateTimeType("${dateString7}T00:00:00.000Z"))
        period7to4.setEndElement(DateTimeType("${dateString4}T00:00:00.000Z"))
        observation7to4.effective = period7to4
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation7to4)).execute()
        assertEquals(2, collection.count())

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)

        // at limit: start
        val dateParam7to3 = DateRangeParam()
        dateParam7to3.lowerBound = DateParam("ge$dateString7")
        dateParam7to3.upperBound = DateParam("lt$dateString3")
        val output7to3 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam7to3
        )
        assertEquals(2, output7to3.size)
        assertEquals("Observation/${observation5to4.id}", output7to3[0].id)
        assertEquals("Observation/${observation7to4.id}", output7to3[1].id)

        // include one, exclude one: start
        val dateParam5to3 = DateRangeParam()
        dateParam5to3.lowerBound = DateParam("ge$dateString5")
        dateParam5to3.upperBound = DateParam("lt$dateString3")
        val output5to3 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam5to3
        )
        assertEquals(1, output5to3.size)
        assertEquals("Observation/${observation5to4.id}", output5to3[0].id)

        // exclude both: start
        val dateParam3toNow = DateRangeParam()
        dateParam3toNow.lowerBound = DateParam("ge$dateString3")
        val output3toNow = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam3toNow
        )
        assertEquals(0, output3toNow.size)

        // at limit: end
        val dateParam8to4 = DateRangeParam()
        dateParam8to4.lowerBound = DateParam("ge$dateString8")
        dateParam8to4.upperBound = DateParam("lt$dateString4")
        val output8to4 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8to4
        )
        assertEquals(2, output8to4.size)
        assertEquals("Observation/${observation5to4.id}", output8to4[0].id)
        assertEquals("Observation/${observation7to4.id}", output8to4[1].id)

        // include one, exclude one: end
        val dateParam6to3 = DateRangeParam()
        dateParam6to3.lowerBound = DateParam("ge$dateString6")
        dateParam6to3.upperBound = DateParam("lt$dateString3")
        val output6to3 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam6to3
        )
        assertEquals(1, output6to3.size)
        assertEquals("Observation/${observation5to4.id}", output6to3[0].id)

        // exclude both: end
        val dateParam5toNow = DateRangeParam()
        dateParam5toNow.upperBound = DateParam("lt$dateString5")
        val output5toNow = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam5toNow
        )
        assertEquals(0, output5toNow.size)
    }

    @Test
    fun `search by date range - date time - nulls`() {
        val prefix = "date-null-"
        collection.remove("true").execute() // Clear the collection in case other tests run first

        // null effective
        val observationNull = Observation()
        observationNull.id = "${prefix}TESTCONDNULL"
        observationNull.subject = Reference("Patient/patient1")
        observationNull.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        observationNull.setEffective(null)
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observationNull)).execute()

        // null effective.value
        val observationNullValue = Observation()
        observationNullValue.id = "${prefix}TESTCONDNULLVALUE"
        observationNullValue.subject = Reference("Patient/patient1")
        observationNullValue.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        observationNullValue.setEffective(DateTimeType(Date())) // null effective.value defaults to "now" in hapi
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observationNullValue)).execute()

        // 7 days ago
        val observation7 = Observation()
        observation7.id = "${prefix}TESTCOND7"
        observation7.subject = Reference("Patient/patient1")
        observation7.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        observation7.setEffective(DateTimeType("${dateString7}T00:00:00.000Z"))
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation7)).execute()
        assertEquals(3, collection.count())

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)

        // non-null range matches DateTime in range & null effective; fails hapi default value "now" when out-of-range
        val dateParam8to4 = DateRangeParam()
        dateParam8to4.lowerBound = DateParam("ge$dateString8")
        dateParam8to4.upperBound = DateParam("lt$dateString4")
        val output8to4 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8to4
        )
        assertEquals(2, output8to4.size)
        assertEquals("Observation/${observationNull.id}", output8to4[0].id)
        assertEquals("Observation/${observation7.id}", output8to4[1].id)

        // null lower, non-null upper matches null effective; non-null upper fails on default "now" & upper bound
        val dateParamNullto5 = DateRangeParam()
        dateParamNullto5.lowerBound = DateParam(null)
        dateParamNullto5.upperBound = DateParam("lt$dateString8")
        val outputNullto5 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParamNullto5
        )
        assertEquals(1, outputNullto5.size)
        assertEquals("Observation/${observationNull.id}", outputNullto5[0].id)

        // null upper, non-null lower matches null effective; non-null lower fails on default "now" & lower bound
        val dateParam5toNull = DateRangeParam()
        dateParam5toNull.lowerBound = DateParam("ge$dateString5")
        dateParam5toNull.upperBound = DateParam(null)
        val output5toNull = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam5toNull
        )
        assertEquals(2, output5toNull.size)
        assertEquals("Observation/${observationNull.id}", output5toNull[0].id)

        // null upper, non-null lower matches null effective, default "now" in range; fails on lower bound outside range
        val dateParam3toNull = DateRangeParam()
        dateParam3toNull.lowerBound = DateParam("ge$dateString3")
        dateParam3toNull.upperBound = DateParam(null)
        val output3toNull = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam3toNull
        )
        assertEquals(2, output3toNull.size)
        assertEquals("Observation/${observationNull.id}", output3toNull[0].id)
        assertEquals("Observation/${observationNullValue.id}", output3toNull[1].id)

        // null lower, non-null upper matches null effective; fails on default "now" out-of-range & upper outside range
        val dateParamNullto8 = DateRangeParam()
        dateParamNullto8.lowerBound = DateParam(null)
        dateParamNullto8.upperBound = DateParam("lt$dateString8")
        val outputNullto8 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParamNullto8
        )
        assertEquals(1, outputNullto8.size)
        assertEquals("Observation/${observationNull.id}", outputNullto8[0].id)
    }

    @Test
    fun `search by date range - period - nulls`() {
        val prefix = "per-null-"
        collection.remove("true").execute() // Clear the collection in case other tests run first

        // null effective
        val observationNull = Observation()
        observationNull.id = "${prefix}TESTCONDNULL"
        observationNull.subject = Reference("Patient/patient1")
        observationNull.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        val periodNull = R4Period()
        periodNull.setStart(null)
        periodNull.setEnd(null)
        observationNull.effective = periodNull
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observationNull)).execute()

        // 7 days ago
        val observation7to4 = Observation()
        observation7to4.id = "${prefix}TESTCOND7TO4"
        observation7to4.subject = Reference("Patient/patient1")
        observation7to4.category = listOf(
            CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        )
        val period7to4 = R4Period()
        period7to4.setStartElement(DateTimeType("${dateString7}T00:00:00.000Z"))
        period7to4.setEndElement(DateTimeType("${dateString4}T00:00:00.000Z"))
        observation7to4.effective = period7to4
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(observation7to4)).execute()
        assertEquals(2, collection.count())

        val tokenMine = TokenParam()
        tokenMine.system = "mySystem"
        tokenMine.value = "myCode"
        val tokenListMine = TokenOrListParam() // "mySystem|myCode"
        tokenListMine.add(tokenMine)

        // populated range matches null effective, date range
        val dateParam8to3 = DateRangeParam()
        dateParam8to3.lowerBound = DateParam("ge$dateString8")
        dateParam8to3.upperBound = DateParam("lt$dateString3")
        val output8to3 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8to3
        )
        assertEquals(2, output8to3.size)
        assertEquals("Observation/${observationNull.id}", output8to3[0].id)
        assertEquals("Observation/${observation7to4.id}", output8to3[1].id)

        // populated range matches null effective, fails on date range too small
        val dateParam6to5 = DateRangeParam()
        dateParam6to5.lowerBound = DateParam("ge$dateString6")
        dateParam6to5.upperBound = DateParam("lt$dateString5")
        val output6to5 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam6to5
        )
        assertEquals(1, output6to5.size)
        assertEquals("Observation/${observationNull.id}", output6to5[0].id)

        // null lower, non-null upper matches null effective, matching upper
        val dateParamNullto3 = DateRangeParam()
        dateParamNullto3.lowerBound = DateParam(null)
        dateParamNullto3.upperBound = DateParam("lt$dateString3")
        val outputNullto3 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParamNullto3
        )
        assertEquals(2, outputNullto3.size)
        assertEquals("Observation/${observationNull.id}", outputNullto3[0].id)
        assertEquals("Observation/${observation7to4.id}", outputNullto3[1].id)

        // null lower, non-null upper matches null effective, fails on out-of-range upper
        val dateParamNullto8 = DateRangeParam()
        dateParamNullto8.lowerBound = DateParam(null)
        dateParamNullto8.upperBound = DateParam("lt$dateString8")
        val outputNullto8 = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParamNullto8
        )
        assertEquals(1, outputNullto8.size)
        assertEquals("Observation/${observationNull.id}", outputNullto8[0].id)

        // non-null lower, null upper matches null effective, matching lower
        val dateParam8toNull = DateRangeParam()
        dateParam8toNull.lowerBound = DateParam("ge$dateString8")
        dateParam8toNull.upperBound = DateParam(null)
        val output8toNull = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam8toNull
        )
        assertEquals(2, output8toNull.size)
        assertEquals("Observation/${observationNull.id}", output8toNull[0].id)
        assertEquals("Observation/${observation7to4.id}", output8toNull[1].id)

        // non-null lower, null upper matches null effective, fails on out-of-range lower
        val dateParam3toNull = DateRangeParam()
        dateParam3toNull.lowerBound = DateParam("ge$dateString3")
        dateParam3toNull.upperBound = DateParam(null)
        val output3toNull = observationProvider.search(
            patientReferenceParam = ReferenceParam("patient1"),
            categoryParam = tokenListMine,
            dateRangeParam = dateParam3toNull
        )
        assertEquals(1, output3toNull.size)
        assertEquals("Observation/${observationNull.id}", output3toNull[0].id)
    }

    @Test
    fun `correct resource returned`() {
        assertEquals(observationProvider.resourceType, Observation::class.java)
    }
}
