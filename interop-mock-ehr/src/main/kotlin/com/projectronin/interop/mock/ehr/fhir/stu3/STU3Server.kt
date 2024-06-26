package com.projectronin.interop.mock.ehr.fhir.stu3

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.RoninVendorFilter
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.stu3.providers.STU3MedicationStatementProvider
import jakarta.servlet.annotation.WebServlet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@WebServlet(urlPatterns = ["/fhir/stu3/*", "/cerner/fhir/stu3/*", "/epic/api/FHIR/STU3/*"])
@Component
class STU3Server(
    @Qualifier("DSTU3") context: FhirContext,
    private val stu3AppointmentProvider: STU3AppointmentResourceProvider,
    private val stu3MedicationStatementProvider: STU3MedicationStatementProvider,
) : RestfulServer(context) {
    override fun initialize() {
        registerInterceptor(RoninVendorFilter())
        setResourceProviders(
            stu3AppointmentProvider,
            stu3MedicationStatementProvider,
        )
        pagingProvider = FifoMemoryPagingProvider(10)
        maximumPageSize = 10 // in reality this is much higher, but this is easier to test with.

        // sets up Swagger/Open API for HAPI fhir
        val openApiInterceptor = OpenApiInterceptor()
        registerInterceptor(openApiInterceptor)
        super.initialize()
    }
}
