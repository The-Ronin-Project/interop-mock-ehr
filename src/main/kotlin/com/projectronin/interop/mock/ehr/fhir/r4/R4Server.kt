package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import javax.servlet.annotation.WebServlet

@WebServlet("/fhir/*")
class R4Server : RestfulServer(FhirContext.forR4()) {

    override fun createPoweredByHeaderProductName(): String {
        return "Ronin"
    }

    override fun initialize() {
        setResourceProviders(R4PatientResourceProvider())
        super.initialize()
    }
}
