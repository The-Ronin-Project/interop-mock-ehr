package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BundleResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CommunicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ConditionResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerRoleResourceProvider
import org.springframework.stereotype.Component
import javax.servlet.annotation.WebServlet

@WebServlet(urlPatterns = ["/fhir/r4/*", "/epic/api/FHIR/R4/*"])
@Component
class R4Server(
    private val r4PatientProvider: R4PatientResourceProvider,
    private val r4ConditionProvider: R4ConditionResourceProvider,
    private val r4AppointmentProvider: R4AppointmentResourceProvider,
    private val r4PractitionerResourceProvider: R4PractitionerResourceProvider,
    private val r4LocationResourceProvider: R4LocationResourceProvider,
    private val r4PractitionerRoleResourceProvider: R4PractitionerRoleResourceProvider,
    private val r4CommunicationResourceProvider: R4CommunicationResourceProvider,
    private val r4BundleResourceProvider: R4BundleResourceProvider
) : RestfulServer(FhirContext.forR4()) {

    override fun initialize() {
        setResourceProviders(
            r4PatientProvider,
            r4ConditionProvider,
            r4AppointmentProvider,
            r4PractitionerResourceProvider,
            r4LocationResourceProvider,
            r4PractitionerRoleResourceProvider,
            r4CommunicationResourceProvider,
            r4BundleResourceProvider
        )
        pagingProvider = FifoMemoryPagingProvider(10)
        maximumPageSize = 10 // in reality this is much higher, but this is easier to test with.

        // sets up Swagger/Open API for HAPI fhir
        val openApiInterceptor = OpenApiInterceptor()
        registerInterceptor(openApiInterceptor)
        super.initialize()
    }
}
