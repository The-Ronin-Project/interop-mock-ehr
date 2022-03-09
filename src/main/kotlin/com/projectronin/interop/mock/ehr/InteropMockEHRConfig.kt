package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InteropMockEHRConfig {
    @Bean
    fun database(): Schema =
        SessionFactory().getSession("mysqlx://localhost:3306/mock_ehr_db?user=springuser&password=ThePassword")
            .defaultSchema
}
