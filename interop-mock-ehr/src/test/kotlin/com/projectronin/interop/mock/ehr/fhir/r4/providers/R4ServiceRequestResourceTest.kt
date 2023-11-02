package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.param.ReferenceParam
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4ServiceRequestDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ServiceRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class R4ServiceRequestResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var serviceRequestProvider: R4ServiceRequestResourceProvider
    private lateinit var dao: R4ServiceRequestDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(ServiceRequest::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(ServiceRequest::class.java) } returns SafeXDev.SafeCollection(
            "resource",
            collection
        )
        every { database.run(any(), captureLambda<Collection.() -> Any>()) } answers {
            val collection = firstArg<SafeXDev.SafeCollection>()
            val lamdba = secondArg<Collection.() -> Any>()
            lamdba.invoke(collection.collection)
        }
        dao = R4ServiceRequestDAO(database, FhirContext.forR4())
        serviceRequestProvider = R4ServiceRequestResourceProvider(dao)
    }

    @Test
    fun `search by patient id - full exact match`() {
        val prefix = "full-"
        val testServiceRequest1 = ServiceRequest()
        testServiceRequest1.subject = Reference("Patient/${prefix}TESTINGID")
        testServiceRequest1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testServiceRequest1)).execute()

        val testServiceRequest2 = ServiceRequest()
        testServiceRequest2.subject = Reference("Patient/${prefix}BADID")
        testServiceRequest2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testServiceRequest2)).execute()

        val output = serviceRequestProvider.search(patientReferenceParam = ReferenceParam("${prefix}TESTINGID"))
        assertEquals(1, output.size)
        assertEquals("ServiceRequest/${testServiceRequest1.id}", output[0].id)
    }

    @Test
    fun `search by patient id - full exact match fails when id does not exist`() {
        val prefix = "exist-"
        val testServiceRequest1 = ServiceRequest()
        testServiceRequest1.subject = Reference("Patient/${prefix}TESTINGID")
        testServiceRequest1.id = "${prefix}TESTCOND1"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testServiceRequest1)).execute()

        val testServiceRequest2 = ServiceRequest()
        testServiceRequest2.subject = Reference("Patient/${prefix}BADID")
        testServiceRequest2.id = "${prefix}TESTCOND2"
        collection.add(FhirContext.forR4().newJsonParser().encodeResourceToString(testServiceRequest2)).execute()

        val output = serviceRequestProvider.search(patientReferenceParam = ReferenceParam("${prefix}OTHERID"))
        assertEquals(0, output.size)
    }

    @Test
    fun `correct resource type`() {
        assertEquals(serviceRequestProvider.resourceType, ServiceRequest::class.java)
    }
}
