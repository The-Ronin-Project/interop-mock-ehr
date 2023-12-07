package com.projectronin.interop.mock.ehr.hl7v2

import ca.uhn.hl7v2.hoh.hapi.server.HohServlet
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4BinaryDAO
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DocumentReferenceDAO
import com.projectronin.interop.mock.ehr.hl7v2.handlers.MDMReceiverHandler
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.DocumentReferenceResolver
import com.projectronin.interop.mock.ehr.hl7v2.resolvers.PatientResolver
import org.springframework.stereotype.Component
import javax.servlet.ServletConfig
import javax.servlet.annotation.WebServlet

@WebServlet(urlPatterns = ["/HL7overHTTP/*"])
@Component
class HL7HTTPServer(
    private val binaryDAO: R4BinaryDAO,
    private val documentReferenceDAO: R4DocumentReferenceDAO,
    private val patientResolver: PatientResolver,
    private val documentReferenceResolver: DocumentReferenceResolver,
) : HohServlet() {
    override fun init(config: ServletConfig) {
        setApplication(MDMReceiverHandler(binaryDAO, documentReferenceDAO, patientResolver, documentReferenceResolver))
    }
}
