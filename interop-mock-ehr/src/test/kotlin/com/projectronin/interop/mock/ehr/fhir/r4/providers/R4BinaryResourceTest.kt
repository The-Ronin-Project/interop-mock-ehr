package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Binary
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4BinaryResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var binaryProvider: R4BinaryResourceProvider

    @BeforeAll
    fun initTest() {
        collection = createCollection(Binary::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Binary::class.java) } returns SafeXDev.SafeCollection(collection)
        val dao = R4BinaryDAO(database, FhirContext.forR4())
        binaryProvider = R4BinaryResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        Assertions.assertEquals(binaryProvider.resourceType, Binary::class.java)
    }
}
