package com.projectronin.interop.mock.ehr

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.LenientErrorHandler
import com.projectronin.interop.mock.ehr.xdevapi.XDevConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class InteropMockEHRConfig {
    @Bean
    fun xdevConfig(
        @Value("#{environment.MOCK_EHR_DB_HOST}") host: String,
        @Value("#{environment.MOCK_EHR_DB_PORT}") port: String,
        @Value("#{environment.MOCK_EHR_DB_NAME}") name: String,
        @Value("#{environment.MOCK_EHR_DB_USER}") user: String,
        @Value("#{environment.MOCK_EHR_DB_PASS}") pass: String
    ): XDevConfig =
        XDevConfig(host, port, name, user, pass)

    @Bean
    @Primary
    fun r4Context(): FhirContext {
        return FhirContext.forR4().setParserErrorHandler(LenientErrorHandler(false))
    }

    @Bean
    @Qualifier("DSTU3")
    fun dstu3Context(): FhirContext {
        return FhirContext.forDstu3().setParserErrorHandler(LenientErrorHandler(false))
    }

    // adds HTTP request logging to the terminal output; very useful
    @Bean
    fun logFilter(): CommonsRequestLoggingFilter? {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        // filter.setIncludePayload(true)
        filter.setIncludePayload(false)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(false)
        filter.setAfterMessagePrefix("REQUEST DATA : ")
        return filter
    }
}
