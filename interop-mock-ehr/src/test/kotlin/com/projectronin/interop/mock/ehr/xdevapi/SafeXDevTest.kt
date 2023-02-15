package com.projectronin.interop.mock.ehr.xdevapi

import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev.SafeCollection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SafeXDevTest {
    private val schema = mockk<Schema>()
    private val xdev = SafeXDev(schema)

    @Test
    fun `createCollection uses schema when not cached`() {
        val xdevCollection = mockk<Collection>()
        every { schema.createCollection("Patient", true) } returns xdevCollection

        val collection = xdev.createCollection(Patient::class.java)
        assertNotNull(collection)

        collection.run {
            assertEquals(xdevCollection, this)
        }
    }

    @Test
    fun `createCollection uses cache when cached`() {
        val collection = mockk<SafeCollection>()

        val mapProperty = SafeXDev::class.memberProperties.find { it.name == "collectionsByResource" }!!
        mapProperty.isAccessible = true

        val map = mapProperty.get(xdev) as MutableMap<Class<out Resource>, SafeCollection>
        map[Patient::class.java] = collection

        val created = xdev.createCollection(Patient::class.java)
        assertEquals(collection, created)
    }

    @Test
    fun `createCollection locks mutex`() {
        val delayTimeInSeconds = 5

        var time1: Long = 0
        var time2: Long = 0
        every { schema.createCollection("Patient", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            time1 = System.currentTimeMillis()
            mockk()
        }
        every { schema.createCollection("Practitioner", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            time2 = System.currentTimeMillis()
            mockk()
        }

        runBlocking {
            awaitAll(
                async {
                    xdev.createCollection(Patient::class.java)
                },
                async {
                    xdev.createCollection(Practitioner::class.java)
                }
            )
        }

        val diff = abs(time1 - time2)
        // If we blocked, then one task will finish at least [delayTimeInSeconds] seconds after the other, but measured in milliseconds.
        assertTrue(time1 != 0L && diff >= (delayTimeInSeconds * 1000))
    }

    @Test
    fun `mutex is not used if collection is already created`() {
        val collection1 = mockk<SafeCollection>()
        val collection2 = mockk<SafeCollection>()

        val mapProperty = SafeXDev::class.memberProperties.find { it.name == "collectionsByResource" }!!
        mapProperty.isAccessible = true

        val map = mapProperty.get(xdev) as MutableMap<Class<out Resource>, SafeCollection>
        map[Patient::class.java] = collection1
        map[Practitioner::class.java] = collection2

        val delayTimeInSeconds = 5
        every { schema.createCollection("Patient", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            mockk()
        }
        every { schema.createCollection("Practitioner", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            mockk()
        }

        val values = runBlocking {
            awaitAll(
                async {
                    xdev.createCollection(Patient::class.java)
                    System.currentTimeMillis()
                },
                async {
                    xdev.createCollection(Practitioner::class.java)
                    System.currentTimeMillis()
                }
            )
        }

        val diff = abs(values.first() - values.last())
        // A bit of leeway here, but this should basically be 0
        assertTrue(diff < 100)
    }

    @Test
    fun `createCollection and SafeCollection run use same mutex`() {
        val delayTimeInSeconds = 5

        var time1: Long = 0
        every { schema.createCollection("Patient", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            time1 = System.currentTimeMillis()
            mockk()
        }

        val collection2 = SafeCollection(mockk())

        var time2: Long = 0
        val task = {
            collection2.run {
                runBlocking {
                    delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
                }
                time2 = System.currentTimeMillis()
            }
        }

        runBlocking {
            awaitAll(
                async {
                    xdev.createCollection(Patient::class.java)
                },
                async {
                    task.invoke()
                }
            )
        }

        val diff = abs(time1 - time2)
        // If we blocked, then one task will finish at least [delayTimeInSeconds] seconds after the other, but measured in milliseconds.
        assertTrue(time1 != 0L && diff >= (delayTimeInSeconds * 1000))
    }
}

class SafeCollectionTest {
    @Test
    fun `run locks collection use`() {
        val collection = mockk<Collection>(relaxed = true)
        val safeCollection = SafeCollection(collection)

        val delayTimeInSeconds = 5
        val task = {
            safeCollection.run {
                runBlocking {
                    delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
                }
                System.currentTimeMillis()
            }
        }

        val values = runBlocking {
            awaitAll(async { task.invoke() }, async { task.invoke() })
        }

        val diff = abs(values.first() - values.last())
        // If we blocked, then one task will finish at least [delayTimeInSeconds] seconds after the other, but measured in milliseconds.
        assertTrue(diff >= (delayTimeInSeconds * 1000))
    }
}
