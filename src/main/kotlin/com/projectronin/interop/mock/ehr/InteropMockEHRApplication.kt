package com.projectronin.interop.mock.ehr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan

@SpringBootApplication
@ServletComponentScan
class InteropMockEHRApplication

fun main(args: Array<String>) {
    runApplication<InteropMockEHRApplication>(*args)
}
