package com.projectronin.interop.mock.ehr.hl7v2.handlers

import ca.uhn.hl7v2.model.v251.datatype.ST
import ca.uhn.hl7v2.model.v251.message.MDM_T02
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.DocumentReferenceResolver
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.PatientResolver
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Enumerations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

internal class MDMReceiverHandlerTest {
    private lateinit var binaryDao: R4BinaryDAO
    private lateinit var documentDao: R4DocumentReferenceDAO
    private lateinit var patientResolver: PatientResolver
    private lateinit var documentResolver: DocumentReferenceResolver
    private lateinit var mdmHandler: MDMReceiverHandler

    private lateinit var message: MDM_T02
    @BeforeEach
    fun init() {
        binaryDao = mockk()
        documentDao = mockk()
        patientResolver = mockk()
        documentResolver = mockk()
        mdmHandler = MDMReceiverHandler(binaryDao, documentDao, patientResolver, documentResolver)
        // not mocking because it is easier to just deal with this rather than mocking all the values
        message = MDM_T02()
        message.msh.fieldSeparator.value = "|"
        message.msh.encodingCharacters.value = "^~\\&"
    }
    @Test
    fun `canProcess - works`() {
        assertTrue(mdmHandler.canProcess(mockk()))
    }

    @Test
    fun `processMessage - handles blank message`() {
        every { documentResolver.findDocumentReference(any()) } returns null
        every { patientResolver.findPatient(any()) } returns null
        val documentSlot = slot<DocumentReference>()
        every { documentDao.insert(capture(documentSlot)) } returns "fake"

        assertNotNull(mdmHandler.processMessage(message, mutableMapOf()))

        val resource = documentSlot.captured
        assertFalse(resource.hasAuthor())
        assertEquals(Enumerations.DocumentReferenceStatus.CURRENT, resource.status)
        assertNull(resource.identifierFirstRep.value)
        assertFalse(resource.hasSubject())
        assertFalse(resource.hasContent())
    }

    @Test
    fun `processMessage - basic document`() {
        every { documentResolver.findDocumentReference(any()) } returns null
        every { patientResolver.findPatient(any()) } returns mockk {
            every { id } returns "patId"
        }
        val documentSlot = slot<DocumentReference>()
        every { documentDao.insert(capture(documentSlot)) } returns "fake"

        val binarySlot = slot<Binary>()
        every { binaryDao.insert(capture(binarySlot)) } returns "binaryId"

        message.txa.uniqueDocumentNumber.entityIdentifier.value = "unique"
        message.txa.insertOriginatorCodeName(0)
        message.txa.originatorCodeName.first().givenName.value = "given"
        message.txa.originatorCodeName.first().familyName.surname.value = "surname"
        message.evn.recordedDateTime.time.value = "20221005"

        message.insertOBSERVATION(0)
        message.insertOBSERVATION(1)
        val allObs = message.observationAll
        allObs[0].obx.valueType.value = "TX"
        val value = ST(message)
        value.value = "Message"
        allObs[0].obx.insertObservationValue(0)
        allObs[0].obx.observationValue.first().data = value

        allObs[1].obx.valueType.value = "Not TX"
        val value2 = ST(message)
        value2.value = "Don't appear"
        allObs[1].obx.insertObservationValue(0)
        allObs[1].obx.observationValue.first().data = value2

        mdmHandler.processMessage(message, mutableMapOf())

        val resource = documentSlot.captured
        assertEquals("given surname", resource.author.first().display)
        assertEquals(Enumerations.DocumentReferenceStatus.CURRENT, resource.status)
        assertEquals("unique", resource.identifierFirstRep.value)
        assertEquals("patId", resource.subject.reference)
        assertEquals("20221005", SimpleDateFormat("yyyyMMdd").format(resource.date))
        assertEquals("Binary/binaryId", resource.content.first().attachment.url)

        val binaryResource = binarySlot.captured
        assertEquals("text/plain", binaryResource.contentTypeElement.value)
        assertEquals("Message", String(binaryResource.content))
    }

    @Test
    fun `processMessage - handles existing binary and document`() {
        val mockExistingDoc = mockk<DocumentReference> {
            every { hasContent() } returns true
            every { content } returns listOf(
                mockk {
                    every { hasAttachment() } returns true
                    every { attachment.hasUrl() } returns true
                    every { attachment.url } returns "Binary/existingBinaryId"
                }

            )
            every { id } returns "existingDocId"
        }
        every { documentResolver.findDocumentReference("unique") } returns mockExistingDoc
        every { patientResolver.findPatient(any()) } returns null
        val documentSlot = slot<DocumentReference>()
        every { documentDao.update(capture(documentSlot)) } just runs

        val binarySlot = slot<Binary>()
        every { binaryDao.update(capture(binarySlot)) } just runs

        message.txa.uniqueDocumentNumber.entityIdentifier.value = "unique"
        message.insertOBSERVATION(0)
        val allObs = message.observationAll
        allObs[0].obx.valueType.value = "TX"
        val value = ST(message)
        value.value = "Message"
        allObs[0].obx.insertObservationValue(0)
        allObs[0].obx.observationValue.first().data = value

        mdmHandler.processMessage(message, mutableMapOf())
        val resource = documentSlot.captured
        val binaryResource = binarySlot.captured

        assertEquals("existingDocId", resource.id)
        assertEquals("existingBinaryId", binaryResource.id)
    }

    @Test
    fun `processMessage - handles existing document but no existing content`() {
        val mockExistingDoc = mockk<DocumentReference> {
            every { hasContent() } returns false
            every { id } returns "existingDocId"
        }
        every { documentResolver.findDocumentReference("unique") } returns mockExistingDoc
        every { patientResolver.findPatient(any()) } returns null
        val documentSlot = slot<DocumentReference>()
        every { documentDao.update(capture(documentSlot)) } just runs

        val binarySlot = slot<Binary>()
        every { binaryDao.update(capture(binarySlot)) } just runs

        message.txa.uniqueDocumentNumber.entityIdentifier.value = "unique"

        mdmHandler.processMessage(message, mutableMapOf())
        val resource = documentSlot.captured
        assertEquals("existingDocId", resource.id)
    }

    @Test
    fun `processMessage - handles existing document but no matching content`() {
        val mockExistingDoc = mockk<DocumentReference> {
            every { hasContent() } returns true
            every { content } returns listOf(
                mockk {
                    every { hasAttachment() } returns true
                    every { attachment.hasUrl() } returns true
                    every { attachment.url } returns null
                }
            )
            every { id } returns "existingDocId"
        }

        every { documentResolver.findDocumentReference("unique") } returns mockExistingDoc
        every { patientResolver.findPatient(any()) } returns null
        val documentSlot = slot<DocumentReference>()
        every { documentDao.update(capture(documentSlot)) } just runs

        val binarySlot = slot<Binary>()
        every { binaryDao.update(capture(binarySlot)) } just runs

        message.txa.uniqueDocumentNumber.entityIdentifier.value = "unique"

        mdmHandler.processMessage(message, mutableMapOf())
        val resource = documentSlot.captured
        assertEquals("existingDocId", resource.id)
    }

    @Test
    fun `processMessage - handles existing document with matching content but no url`() {
        val mockExistingDoc = mockk<DocumentReference> {
            every { hasContent() } returns true
            every { content } returns listOf(
                mockk {
                    every { hasAttachment() } returns false
                },
                mockk {
                    every { hasAttachment() } returns true
                    every { attachment.hasUrl() } returns false
                },
            )
            every { id } returns "existingDocId"
        }

        every { documentResolver.findDocumentReference("unique") } returns mockExistingDoc
        every { patientResolver.findPatient(any()) } returns null
        val documentSlot = slot<DocumentReference>()
        every { documentDao.update(capture(documentSlot)) } just runs

        val binarySlot = slot<Binary>()
        every { binaryDao.update(capture(binarySlot)) } just runs

        message.txa.uniqueDocumentNumber.entityIdentifier.value = "unique"

        mdmHandler.processMessage(message, mutableMapOf())
        val resource = documentSlot.captured
        assertEquals("existingDocId", resource.id)
    }
}
