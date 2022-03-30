package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InteropMockEHRConfig {
    @Bean
    fun database(
        @Value("#{environment.MOCK_EHR_DB_HOST}") host: String,
        @Value("#{environment.MOCK_EHR_DB_PORT}") port: Int,
        @Value("#{environment.MOCK_EHR_DB_NAME}") name: String,
        @Value("#{environment.MOCK_EHR_DB_USER}") user: String,
        @Value("#{environment.MOCK_EHR_DB_PASS}") pass: String
    ): Schema {
        return SessionFactory()
            .getSession("mysqlx://$host:$port/$name?user=$user&password=$pass")
            .defaultSchema
    }
}
