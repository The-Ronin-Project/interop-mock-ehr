package com.projectronin.interop.mock.ehr.fhir.r4

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider
import ca.uhn.fhir.rest.server.RestfulServer
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4AppointmentResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BinaryResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4BundleResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CarePlanResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CareTeamResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4CommunicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ConditionResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4DocumentReferenceResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4EncounterResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4LocationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationRequestResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4MedicationStatementResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4ObservationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4OrganizationResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PatientResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerResourceProvider
import com.projectronin.interop.mock.ehr.fhir.r4.providers.R4PractitionerRoleResourceProvider
import org.springframework.stereotype.Component
import javax.servlet.annotation.WebServlet

@WebServlet(urlPatterns = ["/fhir/r4/*", "/epic/api/FHIR/R4/*"])
@Component
class R4Server(
    context: FhirContext, // autowired
    private val r4PatientProvider: R4PatientResourceProvider,
    private val r4ConditionProvider: R4ConditionResourceProvider,
    private val r4AppointmentProvider: R4AppointmentResourceProvider,
    private val r4PractitionerResourceProvider: R4PractitionerResourceProvider,
    private val r4LocationResourceProvider: R4LocationResourceProvider,
    private val r4PractitionerRoleResourceProvider: R4PractitionerRoleResourceProvider,
    private val r4CommunicationResourceProvider: R4CommunicationResourceProvider,
    private val r4BundleResourceProvider: R4BundleResourceProvider,
    private val r4ObservationResourceProvider: R4ObservationResourceProvider,
    private val r4DocumentReferenceResourceProvider: R4DocumentReferenceResourceProvider,
    private val r4BinaryResourceProvider: R4BinaryResourceProvider,
    private val r4OrganizationResourceProvider: R4OrganizationResourceProvider,
    private val r4CareTeamResourceProvider: R4CareTeamResourceProvider,
    private val r4CarePlanResourceProvider: R4CarePlanResourceProvider,
    private val r4MedicationResourceProvider: R4MedicationResourceProvider,
    private val r4MedicationStatementResourceProvider: R4MedicationStatementResourceProvider,
    private val r4MedicationRequestResourceProvider: R4MedicationRequestResourceProvider,
    private val r4EncounterResourceProvider: R4EncounterResourceProvider,
) : RestfulServer(context) {

    override fun initialize() {
        setResourceProviders(
            r4PatientProvider,
            r4ConditionProvider,
            r4AppointmentProvider,
            r4PractitionerResourceProvider,
            r4LocationResourceProvider,
            r4PractitionerRoleResourceProvider,
            r4CommunicationResourceProvider,
            r4BundleResourceProvider,
            r4ObservationResourceProvider,
            r4DocumentReferenceResourceProvider,
            r4BinaryResourceProvider,
            r4OrganizationResourceProvider,
            r4CareTeamResourceProvider,
            r4CarePlanResourceProvider,
            r4MedicationResourceProvider,
            r4MedicationStatementResourceProvider,
            r4MedicationRequestResourceProvider,
            r4EncounterResourceProvider,
        )
        pagingProvider = FifoMemoryPagingProvider(10)
        maximumPageSize = 10 // in reality this is much higher, but this is easier to test with.

        // sets up Swagger/Open API for HAPI fhir
        val openApiInterceptor = OpenApiInterceptor()
        registerInterceptor(openApiInterceptor)
        super.initialize()
    }
}
