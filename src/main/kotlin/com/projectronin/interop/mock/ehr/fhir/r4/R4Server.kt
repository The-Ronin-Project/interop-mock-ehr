package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import org.springframework.stereotype.Component
import javax.servlet.annotation.WebServlet

@WebServlet("/fhir/r4/*")
@Component
class R4Server(
    private val r4PatientProvider: R4PatientResourceProvider,
    private val r4AppointmentProvider: R4AppointmentResourceProvider
) : RestfulServer(FhirContext.forR4()) {

    override fun initialize() {
        setResourceProviders(r4PatientProvider, r4AppointmentProvider)
        super.initialize()
    }
}