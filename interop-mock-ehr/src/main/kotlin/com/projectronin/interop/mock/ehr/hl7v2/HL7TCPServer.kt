package com.projectronin.interop.mock.ehr.hl7v2

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.app.SimpleServer
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.hl7v2.handlers.MDMReceiverHandler
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.DocumentReferenceResolver
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.PatientResolver
import org.springframework.stereotype.Component

@Component
class HL7TCPServer(
    port: Int = 1011,
    binaryDAO: R4BinaryDAO,
    documentReferenceDAO: R4DocumentReferenceDAO,
    patientResolver: PatientResolver,
    documentReferenceResolver: DocumentReferenceResolver,
) {
    final val server: SimpleServer = DefaultHapiContext().newServer(port, false)

    init {
        val mdmHandler = MDMReceiverHandler(binaryDAO, documentReferenceDAO, patientResolver, documentReferenceResolver)
        server.registerApplication("MDM", "*", mdmHandler)
        // Add handlers for other message structures here
        server.start()
    }
}
