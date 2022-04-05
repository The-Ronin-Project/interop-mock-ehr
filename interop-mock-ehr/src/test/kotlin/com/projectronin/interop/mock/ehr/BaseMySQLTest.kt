package com.projectronin.interop.mock.ehr

import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.SessionFactory
import org.testcontainers.containers.MySQLContainer

abstract class BaseMySQLTest {
    companion object {
        var MYSQL_CONTAINER = MySQLContainer<Nothing>("mysql:8.0.28-oracle").apply {
            withExposedPorts(3306, 33060)
            start()
        }
    }

    fun createCollection(name: String): Collection {
        val address = MYSQL_CONTAINER.host
        val port = MYSQL_CONTAINER.getMappedPort(33060)
        val databaseSession =
            SessionFactory().getSession("mysqlx://$address:$port/test?user=test&password=test").defaultSchema
        return databaseSession.createCollection(name)
    }
}
