package com.projectronin.interop.mock.ehr.fhir.r4.providers

import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.MockEHRBaseTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4CommunicationDAO
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Communication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4CommunicationResourceTest : MockEHRBaseTest() {
    private lateinit var collection: Collection
    private lateinit var communicationProvider: R4CommunicationResourceProvider
    private lateinit var dao: R4CommunicationDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Communication::class.simpleName!!)
        val database = mockk<Schema>()
        every { database.createCollection(Communication::class.simpleName, true) } returns collection
        dao = R4CommunicationDAO(database)
        communicationProvider = R4CommunicationResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        Assertions.assertEquals(communicationProvider.resourceType, Communication::class.java)
    }
}
