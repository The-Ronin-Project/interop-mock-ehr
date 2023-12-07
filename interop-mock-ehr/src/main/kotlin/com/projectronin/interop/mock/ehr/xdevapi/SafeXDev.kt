package com.projectronin.interop.mock.ehr.xdevapi

import com.mysql.cj.exceptions.CJCommunicationsException
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe wrapper around the XDevApi [schema]
 */
@Component
class SafeXDev(private val config: XDevConfig) {
    companion object {
        private val mutex = Mutex()
    }

    private val logger = KotlinLogging.logger { }

    internal var schema: Schema = newSchema()
    private val collectionsByResource: MutableMap<Class<out Resource>, SafeCollection> = ConcurrentHashMap()

    /**
     * Returns a thread-safe Collection for the [resource].
     */
    fun <T : Resource> createCollection(resource: Class<T>): SafeCollection =
        collectionsByResource.computeIfAbsent(resource) {
            runBlocking {
                mutex.withLock {
                    SafeCollection(resource.simpleName, newCollection(resource.simpleName))
                }
            }
        }

    fun <T> run(
        safeCollection: SafeCollection,
        block: Collection.() -> T,
    ): T {
        return runBlocking {
            mutex.withLock {
                testCollection(safeCollection)

                block.invoke(safeCollection.collection)
            }
        }
    }

    private fun testCollection(safeCollection: SafeCollection) {
        val collection = safeCollection.collection
        try {
            collection.count()
        } catch (e: CJCommunicationsException) {
            logger.info(e) { "Communication issue while accessing Collection, so resetting schema and collections" }
            schema = newSchema()
            resetAllCollections()
        }
    }

    private fun newSchema(): Schema =
        SessionFactory().getSession(
            "mysqlx://${config.host}:${config.port}/${config.schema}?user=${config.username}&password=${config.password}",
        )
            .defaultSchema

    private fun resetAllCollections() {
        collectionsByResource.values.forEach {
            it.collection = newCollection(it.resourceName)
        }
    }

    private fun newCollection(resourceName: String): Collection = schema.createCollection(resourceName, true)

    /**
     * A thread-safe wrapper around the given XDevAPI [collection].
     */
    class SafeCollection(internal val resourceName: String, internal var collection: Collection)
}

data class XDevConfig(
    val host: String,
    val port: String,
    val schema: String,
    val username: String,
    val password: String,
)
