package com.projectronin.interop.mock.ehr

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan

@SpringBootApplication
@OpenAPIDefinition(
    info =
        Info(
            title = "Mock EHR APIs",
            version = "1.0",
            description = "REST APIs for all EHR endpoints Ronin needs to interact with",
        ),
    servers = [Server(url = "http://localhost:8080")],
)
@ServletComponentScan
class InteropMockEHRApplication

fun main(args: Array<String>) {
    runApplication<InteropMockEHRApplication>(*args)
}
