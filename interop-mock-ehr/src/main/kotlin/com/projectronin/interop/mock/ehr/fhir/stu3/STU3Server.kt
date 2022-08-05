package com.projectronin.interop.mock.ehr.fhir.stu3

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3PatientResourceProvider
import org.springframework.stereotype.Component
import javax.servlet.annotation.WebServlet

@WebServlet(urlPatterns = ["/fhir/stu3/*", "/epic/api/FHIR/STU3/*"])
@Component
class STU3Server(
    private val stu3AppointmentProvider: STU3AppointmentResourceProvider,
    private val stu3PatientProvider: STU3PatientResourceProvider,
) : RestfulServer(FhirContext.forDstu3()) {

    override fun initialize() {
        setResourceProviders(
            stu3AppointmentProvider,
            stu3PatientProvider
        )
        pagingProvider = FifoMemoryPagingProvider(10)
        maximumPageSize = 10 // in reality this is much higher, but this is easier to test with.

        // sets up Swagger/Open API for HAPI fhir
        val openApiInterceptor = OpenApiInterceptor()
        registerInterceptor(openApiInterceptor)
        super.initialize()
    }
}