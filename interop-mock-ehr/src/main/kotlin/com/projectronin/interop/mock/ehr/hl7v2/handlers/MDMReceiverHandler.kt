package com.projectronin.interop.mock.ehr.hl7v2.handlers

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.datatype.EI
import ca.uhn.hl7v2.model.v251.datatype.XCN
import ca.uhn.hl7v2.model.v251.group.MDM_T02_OBSERVATION
import ca.uhn.hl7v2.model.v251.message.MDM_T02
import ca.uhn.hl7v2.protocol.ReceivingApplication
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.DocumentReferenceResolver
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.PatientResolver
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import java.text.SimpleDateFormat

/*
    Handles HL7v2.5.1 MDM-style messages. Note that MDM_T02 is the structure of the message, not necessarily the event type.
    An MDM_T02 structured messaged can be a T02, T06, T10, etc. event.
 */
class MDMReceiverHandler(
    private val binaryDAO: R4BinaryDAO,
    private val documentReferenceDAO: R4DocumentReferenceDAO,
    private val patientResolver: PatientResolver,
    private val documentReferenceResolver: DocumentReferenceResolver
) : ReceivingApplication<MDM_T02> {
    private val logger = KotlinLogging.logger { }

    /*
        'theMessage' is a HAPI Message object with many convenience functions for retrieving data from the HL7v2 structure.
     */
    override fun processMessage(theMessage: MDM_T02, theMetadata: MutableMap<String, Any>?): Message {
        val binary = buildBinary(theMessage.observationAll)
        var binaryId: String? = null

        // if there's an existing document grab the existing binary to update it
        // this just overwrites w/ w/e was last sent in for a document
        val uniqueID = theMessage.txa.uniqueDocumentNumber.entityIdentifier.value
        val existingDocumentReference = uniqueID?.let { documentReferenceResolver.findDocumentReference(it) }

        if (existingDocumentReference != null) {
            logger.info { "Finding existing binary" }
            binaryId = if (existingDocumentReference.hasContent()) {
                // grab the first attachment that has a url (ok since it's mock EHR and we're creating these)
                val attachment =
                    existingDocumentReference.content.firstOrNull { it.hasAttachment() && it.attachment.hasUrl() }?.attachment
                attachment?.url?.substringAfterLast("/")
            } else null
            // update existing binary
            binary?.id = binaryId
            binary?.let {
                binaryDAO.update(it)
                logger.info { "Updated binary with id: $binaryId" }
            }
            // no existing document so just create the binary
        } else {
            binary?.let {
                binaryId = binaryDAO.insert(it)
                logger.info { "Inserted new binary with id: $binaryId" }
            }
        }
        val document = buildDocumentReference(theMessage, binaryId)

        // if there's an existing document grab that ID to update it, otherwise create a new one
        if (existingDocumentReference != null) {
            document.id = existingDocumentReference.id
            documentReferenceDAO.update(document)
            logger.info { "Updated document with id: ${document.id}" }
        } else {
            val newId = documentReferenceDAO.insert(document)
            logger.info { "Inserted new document with id: $newId" }
        }
        logger.info { "Generating ACK" }
        return theMessage.generateACK()
    }

    override fun canProcess(theMessage: MDM_T02?): Boolean {
        return true // it's unclear why we would ever return false here.
    }

    private fun buildBinary(allObservations: List<MDM_T02_OBSERVATION>?): Binary? {
        // Nothing to create
        if (allObservations.isNullOrEmpty()) return null

        // Turn all the obx values into a list of strings
        val note = allObservations.mapNotNull { observation ->
            val obx = observation.obx
            // only grab those OBX lines where the value is TX
            if (obx.valueType.value == "TX") {
                "${obx.getObservationValue(0).data}"
            } else null
        }

        val binary = Binary()
        binary.contentTypeElement = CodeType("text/plain")
        binary.content = note.reduce { newNote, line -> newNote + "\n$line" }.toByteArray()
        return binary
    }

    private fun buildDocumentReference(theMessage: MDM_T02, binaryId: String?): DocumentReference {
        val originator = theMessage.txa.originatorCodeName.firstOrNull()
        val uniqueId = theMessage.txa.uniqueDocumentNumber
        val creationDate = theMessage.evn.recordedDateTime.time.value

        val patient = patientResolver.findPatient(theMessage.pid)

        val document = DocumentReference()
        document.status = Enumerations.DocumentReferenceStatus.CURRENT
        document.author = originator?.let { listOf(it.toReference()) }
        document.identifier = listOf(uniqueId.toIdentifier())
        document.subject = patient?.toReference()
        document.date = creationDate?.let { SimpleDateFormat("yyyyMMdd").parse(it) }

        // only make a reference if we can reference something!
        if (binaryId != null) {
            val content = DocumentReference.DocumentReferenceContentComponent()
            content.attachment = Attachment()
            content.attachment.url = "Binary/$binaryId"
            document.content = listOf(content)
        }

        return document
    }

    // some helper functions that could be reused eventually and make the code cleaner
    private fun Patient.toReference(): Reference {
        val reference = Reference()
        reference.type = "Patient"
        reference.reference = this.id
        return reference
    }

    private fun XCN.toReference(): Reference {
        val reference = Reference()
        reference.display = this.givenName?.value + " " + this.familyName?.surname?.value
        return reference
    }

    private fun EI.toIdentifier(): Identifier {
        val identifier = Identifier()
        identifier.value = this.entityIdentifier?.value

        val coding = CodeableConcept()
        coding.text = "Unique ID"
        identifier.type = coding

        return identifier
    }
}
