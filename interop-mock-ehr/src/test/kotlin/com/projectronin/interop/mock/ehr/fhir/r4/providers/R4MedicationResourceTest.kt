package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.context.FhirContext
import com.mysql.cj.xdevapi.Collection
import com.projectronin.interop.mock.ehr.BaseMySQLTest
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4MedicationDAO
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Medication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R4MedicationResourceTest : BaseMySQLTest() {
    private lateinit var collection: Collection
    private lateinit var medicationProvider: R4MedicationResourceProvider
    private lateinit var dao: R4MedicationDAO

    @BeforeAll
    fun initTest() {
        collection = createCollection(Medication::class.simpleName!!)
        val database = mockk<SafeXDev>()
        every { database.createCollection(Medication::class.java) } returns
            SafeXDev.SafeCollection(
                "resource",
                collection,
            )
        dao = R4MedicationDAO(database, FhirContext.forR4())
        medicationProvider = R4MedicationResourceProvider(dao)
    }

    @Test
    fun `correct resource type`() {
        Assertions.assertEquals(medicationProvider.resourceType, Medication::class.java)
    }
}
