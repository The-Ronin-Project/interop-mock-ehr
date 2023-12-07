package com.projectronin.interop.mock.ehr.fhir.r4.providers

import ca.uhn.fhir.rest.annotation.OptionalParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import com.projectronin.interop.mock.ehr.fhir.r4.dao.R4DiagnosticReportDAO
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Procedure
import org.springframework.stereotype.Component

@Component
class R4DiagnosticReportResourceProvider(
    override var resourceDAO: R4DiagnosticReportDAO,
) : BaseResourceProvider<DiagnosticReport, R4DiagnosticReportDAO>() {
    override fun getResourceType(): Class<out IBaseResource> {
        return DiagnosticReport::class.java
    }

    @Search
    fun search(
        @OptionalParam(name = Procedure.SP_PATIENT) patient: ReferenceParam,
        @OptionalParam(name = Procedure.SP_DATE) dateRangeParam: DateRangeParam? = null,
        @OptionalParam(name = "-timing-boundsPeriod") cernerDateRangeParam: DateRangeParam? = null,
    ): List<DiagnosticReport> {
        if (dateRangeParam != null && cernerDateRangeParam != null) {
            throw UnsupportedOperationException(
                "The DiagnosticReport endpoint does not allow both optional parameters " +
                    "\"${DiagnosticReport.SP_DATE}\" and \"-timing-boundsPeriod\" to be specified.",
            )
        }

        val dateRange = dateRangeParam ?: cernerDateRangeParam

        return resourceDAO.searchByQuery(
            patient.value,
            dateRange?.lowerBoundAsInstant,
            dateRange?.upperBoundAsInstant,
        )
    }
}
