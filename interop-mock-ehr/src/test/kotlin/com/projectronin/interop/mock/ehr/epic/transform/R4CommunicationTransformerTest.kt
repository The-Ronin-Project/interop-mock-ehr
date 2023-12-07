package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import org.hl7.fhir.r4.model.Communication
import org.hl7.fhir.r4.model.ResourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class R4CommunicationTransformerTest {
    @Test
    fun `can build communication`() {
        val sendMessageRequest =
            SendMessageRequest(
                messageText = listOf("Message Text", "Line 2"),
                patientID = "MRN#1",
                recipients =
                    listOf(
                        SendMessageRecipient("first", false, "External"),
                        SendMessageRecipient("second", true, "External"),
                    ),
                senderID = "Sender#1",
                messageType = "messageType",
                senderIDType = "SendType#1",
                patientIDType = "MRN",
                contactID = "Con#1",
                contactIDType = "ConType#1",
                messagePriority = "urgent",
            )

        val communication = R4CommunicationTransformer().transformFromSendMessage(sendMessageRequest)
        assertNotNull(communication)

        assertEquals(1, communication.category.size)
        assertEquals("messageType", communication.category.first().text)

        assertEquals(1, communication.payload.size)
        assertEquals("Message Text\nLine 2", communication.payload.first().content.toString())

        assertEquals(Communication.CommunicationStatus.COMPLETED, communication.status)
        assertEquals(Communication.CommunicationPriority.URGENT, communication.priority)

        assertEquals(ResourceType.Patient.name, communication.subject.type)
        assertEquals("MRN#1", communication.subject.identifier.value)
        assertEquals("MRN", communication.subject.identifier.type.text)

        assertEquals(ResourceType.Encounter.name, communication.encounter.type)
        assertEquals("Con#1", communication.encounter.identifier.value)
        assertEquals("ConType#1", communication.encounter.identifier.type.text)

        assertEquals(ResourceType.Organization.name, communication.sender.type)
        assertEquals("Sender#1", communication.sender.identifier.value)
        assertEquals("SendType#1", communication.sender.identifier.type.text)

        assertEquals(2, communication.recipient.size)
        val reference1 = communication.recipient[0]
        val reference2 = communication.recipient[1]
        assertEquals("first", reference1.identifier.value)
        assertEquals("External", reference1.identifier.type.text)
        assertEquals(ResourceType.Practitioner.name, reference1.type)
        assertEquals(ResourceType.Group.name, reference2.type)
    }

    @Test
    fun `message text is optional`() {
        val sendMessageRequest =
            SendMessageRequest(
                messageText = null,
                patientID = "MRN#1",
                recipients =
                    listOf(
                        SendMessageRecipient("first", false, "External"),
                        SendMessageRecipient("second", true, "External"),
                    ),
                senderID = "Sender#1",
                messageType = "messageType",
                senderIDType = "SendType#1",
                patientIDType = "MRN",
                contactID = "Con#1",
                contactIDType = "ConType#1",
                messagePriority = "just incoherent gibberish",
            )

        val communication = R4CommunicationTransformer().transformFromSendMessage(sendMessageRequest)
        assertNotNull(communication)
        assertEquals(1, communication.payload.size)
        assertEquals("", communication.payload.first().content.toString())
    }

    @Test
    fun `priority defaults`() {
        val sendMessageRequest =
            SendMessageRequest(
                messageText = listOf("Message Text", "Line 2"),
                patientID = "MRN#1",
                recipients =
                    listOf(
                        SendMessageRecipient("first", false, "External"),
                        SendMessageRecipient("second", true, "External"),
                    ),
                senderID = "Sender#1",
                messageType = "messageType",
                senderIDType = "SendType#1",
                patientIDType = "MRN",
                contactID = "Con#1",
                contactIDType = "ConType#1",
                messagePriority = "just incoherent gibberish",
            )

        val communication = R4CommunicationTransformer().transformFromSendMessage(sendMessageRequest)
        assertNotNull(communication)
        assertEquals(Communication.CommunicationPriority.ROUTINE, communication.priority)
    }
}
