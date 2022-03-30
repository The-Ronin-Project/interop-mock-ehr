package com.projectronin.interop.mock.ehr.testcontainer

import org.testcontainers.containers.DockerComposeContainer
import java.io.File

class InteropMockEHRTestContainer() :
    DockerComposeContainer<InteropMockEHRTestContainer>(File(InteropMockEHRTestContainer::class.java.classLoader.getResource("mock-ehr-testcontainer.yml")!!.toURI())) {
    init {
        this.withExposedService("mockehr", 8080)
    }

    fun getURL(): String {
        return "${this.getServiceHost("mockehr",8080)}:${this.getServicePort("mockehr",8080)}"
    }
}
