package com.projectronin.interop.mock.ehr.epic.transform

import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Communication
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.springframework.stereotype.Component

@Component
class R4CommunicationTransformer {
    fun transformFromSendMessage(sendMessageRequest: SendMessageRequest): Communication {
        val messageContent =
            Communication.CommunicationPayloadComponent(
                StringType(
                    sendMessageRequest.messageText?.joinToString("\n") ?: "",
                ),
            )
        val messageType = CodeableConcept()
        messageType.text = sendMessageRequest.messageType

        val communicationPriority =
            try {
                Communication.CommunicationPriority.fromCode(sendMessageRequest.messagePriority)
            } catch (e: FHIRException) {
                Communication.CommunicationPriority.ROUTINE
            }

        val patientReference =
            buildReference(
                ResourceType.Patient,
                sendMessageRequest.patientID,
                sendMessageRequest.patientIDType,
            )
        val senderReference =
            buildReference(
                ResourceType.Organization,
                sendMessageRequest.senderID,
                sendMessageRequest.senderIDType,
            )
        val encounterReference =
            buildReference(
                ResourceType.Encounter,
                sendMessageRequest.contactID,
                sendMessageRequest.contactIDType,
            )
        val recipients =
            sendMessageRequest.recipients?.map {
                val referenceType =
                    when (it.isPool) {
                        true -> ResourceType.Group
                        false -> ResourceType.Practitioner
                    }
                buildReference(referenceType, it.iD, it.iDType)
            }

        val communication = Communication()
        communication.payload = listOf(messageContent)
        communication.category = listOf(messageType)
        communication.priority = communicationPriority
        communication.status = Communication.CommunicationStatus.COMPLETED
        communication.subject = patientReference
        communication.sender = senderReference
        communication.encounter = encounterReference
        communication.recipient = recipients
        return communication
    }

    private fun buildReference(
        referenceType: ResourceType,
        id: String?,
        idType: String?,
    ): Reference {
        val identifier = Identifier()
        identifier.type.text = idType
        identifier.value = id
        val reference = Reference()
        reference.type = referenceType.name
        reference.identifier = identifier
        return reference
    }
}
