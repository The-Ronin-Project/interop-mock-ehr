package com.projectronin.interop.mock.ehr.testcontainer

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.Network

abstract class BaseMockEHRTest {
    companion object {

        // needs to start before MOCK_EHR_CONTAINER, but isn't used directly.
        @Suppress("unused")
        var MYSQL_CONTAINER = MySQLContainer<Nothing>("mysql:8.0.28-oracle").apply {
            withExposedPorts(3306, 33060)
            withNetwork(Network.SHARED)
            withNetworkAliases("database")
            start()
        }

        var MOCK_EHR_CONTAINER = GenericContainer("docker-proxy.devops.projectronin.io/interop-mock-ehr:latest").apply {
            withExposedPorts(8080)
            withNetwork(Network.SHARED)
            withEnv(
                mapOf(
                    "MOCK_EHR_DB_HOST" to "database",
                    "MOCK_EHR_DB_PORT" to "33060",
                    "MOCK_EHR_DB_NAME" to "test", // default values from MySQLContainer
                    "MOCK_EHR_DB_USER" to "test",
                    "MOCK_EHR_DB_PASS" to "test"
                )
            )
            start()
        }
    }

    fun getURL(): String {
        val address = MOCK_EHR_CONTAINER.host
        val port = MOCK_EHR_CONTAINER.getMappedPort(8080)
        return "http://$address:$port"
    }
}
