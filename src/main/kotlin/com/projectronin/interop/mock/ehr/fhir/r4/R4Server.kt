package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import org.springframework.stereotype.Component
import javax.servlet.annotation.WebServlet

@WebServlet("/fhir/r4/*")
@Component
class R4Server(
    private val r4PatientProvider: R4PatientResourceProvider,
    private val r4AppointmentProvider: R4AppointmentResourceProvider,
    private val r4PractitionerResourceProvider: R4PractitionerResourceProvider,
    private val r4LocationResourceProvider: R4LocationResourceProvider
) : RestfulServer(FhirContext.forR4()) {

    override fun initialize() {
        setResourceProviders(
            r4PatientProvider,
            r4AppointmentProvider,
            r4PractitionerResourceProvider,
            r4LocationResourceProvider
        )
        super.initialize()
    }
}
