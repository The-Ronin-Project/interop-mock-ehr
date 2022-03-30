package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.SessionFactory
import org.testcontainers.containers.MySQLContainer

fun getTestCollection(): Collection {
    val actualFakeDatabase = MySQLContainer<Nothing>("mysql:8.0.28-oracle").apply {
        withExposedPorts(3306, 33060) // 33060 is the ProtocolX port, which has document store
    }
    actualFakeDatabase.start()
    val address = actualFakeDatabase.host
    val port = actualFakeDatabase.getMappedPort(33060)
    val databaseSession =
        SessionFactory().getSession("mysqlx://$address:$port/test?user=test&password=test").defaultSchema
    return databaseSession.createCollection("test")
}
