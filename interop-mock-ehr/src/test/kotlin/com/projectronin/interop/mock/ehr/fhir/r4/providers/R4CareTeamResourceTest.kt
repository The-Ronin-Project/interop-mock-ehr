package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.StringParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CareTeamDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.CareTeam
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4CareTeamResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var careTeamProvider: R4CareTeamResourceProvider
    private lateinit var dao: R4CareTeamDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(CareTeam::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(CareTeam::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4CareTeamDAO(database, FhirContext.forR4())
        careTeamProvider = R4CareTeamResourceProvider(dao)
    }

    @Test
    fun `search by patient id - full exact match`() {
        val prefix = "full-"
        val testCareTeam1 = CareTeam()
        testCareTeam1.subject = Reference("Patient/${prefix}TESTINGID")
        testCareTeam1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam1)).execute()

        val testCareTeam2 = CareTeam()
        testCareTeam2.subject = Reference("Patient/${prefix}BADID")
        testCareTeam2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam2)).execute()

        val output = careTeamProvider.search(patientReferenceParam = ReferenceParam("${prefix}TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("CareTeam/${testCareTeam1.id}", output[0].id)
    }

    @Test
    fun `search by patient id - full exact match fails when id does not exist`() {
        val prefix = "exist-"
        val testCareTeam1 = CareTeam()
        testCareTeam1.subject = Reference("Patient/${prefix}TESTINGID")
        testCareTeam1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam1)).execute()

        val testCareTeam2 = CareTeam()
        testCareTeam2.subject = Reference("Patient/${prefix}BADID")
        testCareTeam2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam2)).execute()

        val output = careTeamProvider.search(patientReferenceParam = ReferenceParam("${prefix}OTHERID"))
        assertEquals(0, output.size)
    }

    @Test
    fun `search by status`() {
        val prefix = "full-clin-"
        collection.remove("true").execute() // Clear the collection in case other tests run first
        assertEquals(collection.find().execute().count(), 0)

        val testCareTeam1 = CareTeam()
        testCareTeam1.status = CareTeam.CareTeamStatus.ACTIVE
        testCareTeam1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam1)).execute()

        val testCareTeam2 = CareTeam()
        testCareTeam2.status = CareTeam.CareTeamStatus.ENTEREDINERROR
        testCareTeam2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam2)).execute()

        val testCareTeam3 = CareTeam()
        testCareTeam3.status = CareTeam.CareTeamStatus.INACTIVE
        testCareTeam3.id = "${prefix}TESTCOND3"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam3)).execute()

        val testCareTeam4 = CareTeam()
        testCareTeam4.status = CareTeam.CareTeamStatus.ACTIVE
        testCareTeam4.id = "${prefix}TESTCOND4"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam4)).execute()

        val testCareTeam5 = CareTeam()
        testCareTeam5.status = CareTeam.CareTeamStatus.ACTIVE
        testCareTeam5.id = "${prefix}TESTCOND5"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testCareTeam5)).execute()

        val output = careTeamProvider.search(statusParam = StringParam("active"))
        assertEquals(3, output.size)
        assertEquals("CareTeam/${testCareTeam1.id}", output[0].id)
        assertEquals("CareTeam/${testCareTeam4.id}", output[1].id)
        assertEquals("CareTeam/${testCareTeam5.id}", output[2].id)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(careTeamProvider.resourceType, CareTeam::class.java)
    }

    @Test
    fun `code coverage test using dao searchByQuery`() {
        dao.searchByQuery() // use all default parameter values
    }
}
