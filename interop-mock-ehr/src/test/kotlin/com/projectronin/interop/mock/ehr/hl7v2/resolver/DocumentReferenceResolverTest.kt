package com.projectronin.interop.mock.ehr.hl7v2.resolver

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.DocumentReferenceResolver
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.DocumentReference
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DocumentReferenceResolverTest {
    private lateinit var documentDAO: R4DocumentReferenceDAO

    @BeforeEach
    fun init() {
        documentDAO = mockk()
    }

    @Test
    fun `findDocumentReference - can resolve`() {
        val resolver = DocumentReferenceResolver(documentDAO)
        every { documentDAO.searchByIdentifier(match { it.value == "123" }) } returns DocumentReference()
        assertNotNull(resolver.findDocumentReference("123"))
    }

    @Test
    fun `findPatient - can return null`() {
        val resolver = DocumentReferenceResolver(documentDAO)
        every { documentDAO.searchByIdentifier(any()) } returns null
        assertNull(resolver.findDocumentReference("123"))
    }
}
