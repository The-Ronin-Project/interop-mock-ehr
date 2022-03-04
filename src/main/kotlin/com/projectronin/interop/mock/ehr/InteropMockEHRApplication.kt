package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan

@SpringBootApplication
@ServletComponentScan
class InteropMockEHRApplication {

    companion object Companion {
        val database: Schema =
            SessionFactory().getSession("mysqlx://localhost:3306/mock_ehr_db?user=springuser&password=ThePassword")
                .defaultSchema
        // TODO: Put these values somewhere instead of hardcoding
    }
}

fun main(args: Array<String>) {
    runApplication<InteropMockEHRApplication>(*args)
}
