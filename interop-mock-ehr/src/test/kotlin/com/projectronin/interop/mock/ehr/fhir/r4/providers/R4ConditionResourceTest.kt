package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.TokenOrListParam
import ca.uhn.fhir.rest.param.TokenParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ConditionDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4ConditionResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var conditionProvider: R4ConditionResourceProvider
    private lateinit var dao: R4ConditionDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Condition::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Condition::class.java) } returns SafeXDev.SafeCollection(collection)
        dao = R4ConditionDAO(database, FhirContext.forR4())
        conditionProvider = R4ConditionResourceProvider(dao)
    }

    @Test
    fun `search by patient id - full exact match`() {
        val prefix = "full-"
        val testCondition1 = Condition()
        testCondition1.subject = Reference("Patient/${prefix}TESTINGID")
        testCondition1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition1)).execute()

        val testCondition2 = Condition()
        testCondition2.subject = Reference("Patient/${prefix}BADID")
        testCondition2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition2)).execute()

        val output = conditionProvider.search(patientReferenceParam = ReferenceParam("${prefix}TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("Condition/${testCondition1.id}", output[0].id)
    }

    @Test
    fun `search by patient id - full exact match fails when id does not exist`() {
        val prefix = "exist-"
        val testCondition1 = Condition()
        testCondition1.subject = Reference("Patient/${prefix}TESTINGID")
        testCondition1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition1)).execute()

        val testCondition2 = Condition()
        testCondition2.subject = Reference("Patient/${prefix}BADID")
        testCondition2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition2)).execute()

        val output = conditionProvider.search(patientReferenceParam = ReferenceParam("${prefix}OTHERID"))
        assertEquals(0, output.size)
    }

    @Test
    fun `search by category - category(n)_coding(n)_code or category(n)_text`() {
        val prefix = "full-cat-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testCondition1 = Condition()
        testCondition1.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testCondition1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition1)).execute()

        val testCondition2 = Condition()
        testCondition2.category = listOf(CodeableConcept(Coding("mySystem", "otherCode", "otherDisplay")))
        testCondition2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition2)).execute()

        val testCondition3 = Condition()
        testCondition3.category = listOf(CodeableConcept(Coding("mySystem", "myCode", "myDisplay")))
        testCondition3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition3)).execute()

        val testCondition4 = Condition()
        var codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "myCode"
        testCondition4.category = listOf(codeableConcept4)
        testCondition4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition4)).execute()

        val testCondition5 = Condition()
        var codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "otherCode"
        testCondition5.category = listOf(codeableConcept5, codeableConcept4)
        testCondition5.id = "${prefix}TESTCOND5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition5)).execute()

        val testCondition6 = Condition()
        testCondition6.category = listOf(codeableConcept5)
        testCondition6.id = "${prefix}TESTCOND6"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition6)).execute()

        val testCondition7 = Condition()
        testCondition7.category = listOf()
        testCondition7.id = "${prefix}TESTCOND7"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition7)).execute()

        val output = conditionProvider.search(categoryParam = TokenOrListParam("", "myCode", "otherCode"))
        assertEquals(6, output.size)
    }

    @Test
    fun `search by clinicalStatus - clinicalStatus_coding(n)_code or clinicalStatus_text`() {
        val prefix = "full-clin-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testCondition1 = Condition()
        testCondition1.clinicalStatus = CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        testCondition1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition1)).execute()

        val testCondition2 = Condition()
        testCondition2.clinicalStatus = CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        testCondition2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition2)).execute()

        val testCondition3 = Condition()
        testCondition3.clinicalStatus = CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        testCondition3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition3)).execute()

        val testCondition4 = Condition()
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "myCode"
        testCondition4.clinicalStatus = codeableConcept4
        testCondition4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition4)).execute()

        val testCondition5 = Condition()
        val codeableConcept5 = CodeableConcept()
        codeableConcept5.text = "otherCode"
        testCondition5.clinicalStatus = codeableConcept5
        testCondition5.id = "${prefix}TESTCOND5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition5)).execute()

        val output = conditionProvider.search(clinicalStatusParam = TokenParam("system", "myCode"))
        assertEquals(3, output.size)
        assertEquals("Condition/${testCondition1.id}", output[0].id)
        assertEquals("Condition/${testCondition3.id}", output[1].id)
        assertEquals("Condition/${testCondition4.id}", output[2].id)
    }

    @Test
    fun `search by multiple conditions`() {
        val prefix = "mixed-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        // "myCode" matches are 1 and 3
        val codeableConcept1 = CodeableConcept(Coding("mySystem", "myCode", "myDisplay"))
        val codeableConcept2 = CodeableConcept(Coding("otherSystem", "otherCode", "otherDisplay"))
        val codeableConcept3 = CodeableConcept()
        codeableConcept3.text = "myCode"
        val codeableConcept4 = CodeableConcept()
        codeableConcept4.text = "otherCode"

        val testConditionNoMatch = Condition()
        testConditionNoMatch.clinicalStatus = codeableConcept2
        testConditionNoMatch.category = listOf(codeableConcept4, codeableConcept2)
        testConditionNoMatch.id = "${prefix}TESTCONDNOMATCH1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testConditionNoMatch)).execute()

        // AND match on "myCode"
        val testCondition1 = Condition()
        testCondition1.clinicalStatus = codeableConcept1
        testCondition1.category = listOf(codeableConcept3)
        testCondition1.severity = codeableConcept1
        testCondition1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition1)).execute()

        val testCondition2 = Condition()
        testCondition2.clinicalStatus = codeableConcept2
        testCondition2.category = listOf(codeableConcept1)
        testCondition2.severity = codeableConcept3
        testCondition2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition2)).execute()

        val testCondition3 = Condition()
        testCondition3.clinicalStatus = codeableConcept4
        testCondition3.category = listOf(codeableConcept3)
        testCondition3.severity = codeableConcept1
        testCondition3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition3)).execute()

        testConditionNoMatch.id = "${prefix}TESTCONDNOMATCH2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testConditionNoMatch)).execute()

        // AND match on "myCode"
        val testCondition4 = Condition()
        testCondition4.clinicalStatus = codeableConcept3
        testCondition4.category = listOf(codeableConcept1)
        testCondition4.severity = codeableConcept3
        testCondition4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCondition4)).execute()

        // AND match on "myCode" for 3 mixed properties matches only conditions 1 and 4
        var output = conditionProvider.search(
            clinicalStatusParam = TokenParam("system", "myCode"),
            categoryParam = TokenOrListParam("", "myCode")
        )
        assertEquals(2, output.size)
        assertEquals("Condition/${testCondition1.id}", output[0].id)
        assertEquals("Condition/${testCondition4.id}", output[1].id)

        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)
        testConditionNoMatch.id = "${prefix}TESTCONDNOMATCH3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testConditionNoMatch)).execute()
        testConditionNoMatch.id = "${prefix}TESTCONDNOMATCH4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testConditionNoMatch)).execute()
        testConditionNoMatch.id = "${prefix}TESTCONDNOMATCH5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testConditionNoMatch)).execute()

        // no match for mixed AND condition
        output = conditionProvider.search(
            clinicalStatusParam = TokenParam("system", "myCode"),
            categoryParam = TokenOrListParam("", "myCode")
        )
        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(conditionProvider.resourceType, Condition::class.java)
    }

    @Test
    fun `code coverage test using dao searchByQuery`() {
        dao.searchByQuery() // use all default parameter values
    }
}
