package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4FlagDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4FlagResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var flagProvider: R4FlagResourceProvider
    private lateinit var dao: R4FlagDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Flag::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Flag::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4FlagDAO(database, FhirContext.forR4())
        flagProvider = R4FlagResourceProvider(dao)
    }

    @Test
    fun `search by patient id - full exact match`() {
        val prefix = "full-"
        val testFlag1 = Flag()
        testFlag1.subject = Reference("Patient/${prefix}TESTINGID")
        testFlag1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testFlag1)).execute()

        val testFlag2 = Flag()
        testFlag2.subject = Reference("Patient/${prefix}BADID")
        testFlag2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testFlag2)).execute()

        val output = flagProvider.search(patientReferenceParam = ReferenceParam("${prefix}TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("Flag/${testFlag1.id}", output[0].id)
    }

    @Test
    fun `search by patient id - full exact match fails when id does not exist`() {
        val prefix = "exist-"
        val testFlag1 = Flag()
        testFlag1.subject = Reference("Patient/${prefix}TESTINGID")
        testFlag1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testFlag1)).execute()

        val testFlag2 = Flag()
        testFlag2.subject = Reference("Patient/${prefix}BADID")
        testFlag2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testFlag2)).execute()

        val output = flagProvider.search(patientReferenceParam = ReferenceParam("${prefix}OTHERID"))
        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(flagProvider.resourceType, Flag::class.java)
    }
}
