package com.projectronin.interop.mock.ehr.testcontainer

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.Network
import org.testcontainers.images.PullPolicy

@Service
class MockEHRTestcontainer {
    private var httpClient = HttpClient(CIO)

    companion object {
        private val sharedNetwork = Network.newNetwork()
        // needs to start before MOCK_EHR_CONTAINER, but isn't used directly.
        @Suppress("unused")
        var MYSQL_CONTAINER = MySQLContainer<Nothing>("mysql:8.0.28-oracle").apply {
            withExposedPorts(3306, 33060)
            withNetwork(sharedNetwork)
            withNetworkAliases("database")
            start()
        }

        var MOCK_EHR_CONTAINER = GenericContainer("docker-proxy.devops.projectronin.io/interop-mock-ehr:latest").apply {
            withExposedPorts(8080)
            withNetwork(sharedNetwork)
            withImagePullPolicy(PullPolicy.alwaysPull()) // 'latest' may be updated frequently
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

    /**
     * Retrieves base URL for MockEHR Spring Application
     */
    fun getURL(): String {
        val address = MOCK_EHR_CONTAINER.host
        val port = MOCK_EHR_CONTAINER.getMappedPort(8080)
        return "http://$address:$port"
    }

    /**
     * Convenience function for adding an R4 resource to the MockEHR database.
     * @param type  the type of resource, e.g. "Patient"
     * @param json  the json body of the resource to add. must include 'id' and 'resourceType'.
     * @param fhirID    the fhir id of the object. must exactly match 'id' in the json body.
     */
    fun addR4Resource(type: String, json: String, fhirID: String): HttpResponse {
        return runBlocking {
            httpClient.put<HttpResponse>("${getURL()}/fhir/r4/$type/$fhirID") {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                body = json
            }
        }
    }
}
