package com.projectronin.interop.mock.ehr.hl7v2.resolvers

import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import mu.KotlinLogging
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Identifier
import org.springframework.stereotype.Component

/***
 * Class for resolving HL7v2 components into FHIR DocumentReference objects
 */
@Component
class DocumentReferenceResolver(private val documentReferenceDAO: R4DocumentReferenceDAO) {
    private val logger = KotlinLogging.logger { }

    /**
     * Finds an existing document reference given a [uniqueId] which should be coming from TXA-12
     */
    fun findDocumentReference(uniqueId: String): DocumentReference? {
        logger.info { "Searching for document" }
        val identifier = Identifier()
        identifier.value = uniqueId
        return documentReferenceDAO.searchByIdentifier(identifier)
    }
}
