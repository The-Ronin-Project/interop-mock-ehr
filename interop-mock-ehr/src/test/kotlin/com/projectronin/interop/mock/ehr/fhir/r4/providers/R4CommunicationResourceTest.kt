package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CommunicationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Communication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4CommunicationResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var communicationProvider: R4CommunicationResourceProvider
    private lateinit var dao: R4CommunicationDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Communication::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Communication::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        dao = R4CommunicationDAO(database, FhirContext.forR4())
        communicationProvider = R4CommunicationResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        Assertions.assertEquals(communicationProvider.resourceType, Communication::class.java)
    }
}
