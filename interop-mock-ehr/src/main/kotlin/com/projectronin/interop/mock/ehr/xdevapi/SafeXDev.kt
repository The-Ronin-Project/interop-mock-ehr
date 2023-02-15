package com.projectronin.interop.mock.ehr.xdevapi

import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hl7.fhir.r4.model.Resource
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe wrapper around the XDevApi [schema]
 */
class SafeXDev(private val schema: Schema) {
    companion object {
        private val mutex = Mutex()
    }

    private val collectionsByResource: MutableMap<Class<out Resource>, SafeCollection> = ConcurrentHashMap()

    /**
     * Returns a thread-safe Collection for the [resource].
     */
    fun <T : Resource> createCollection(resource: Class<T>): SafeCollection =
        collectionsByResource.computeIfAbsent(resource) {
            runBlocking {
                mutex.withLock {
                    SafeCollection(schema.createCollection(resource.simpleName, true))
                }
            }
        }

    /**
     * A thread-safe wrapper around the given XDevAPI [collection].
     */
    class SafeCollection(private val collection: Collection) {
        /**
         * Runs the supplied [block] on this Collection.
         */
        fun <T> run(block: Collection.() -> T): T {
            return runBlocking {
                mutex.withLock {
                    block.invoke(collection)
                }
            }
        }
    }
}
