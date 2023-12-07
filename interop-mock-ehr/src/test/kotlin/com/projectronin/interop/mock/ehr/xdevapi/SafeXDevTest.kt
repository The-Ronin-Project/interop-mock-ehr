package com.projectronin.interop.mock.ehr.xdevapi

import com.mysql.cj.exceptions.CJCommunicationsException
import com.mysql.cj.xdevapi.Collection
import com.mysql.cj.xdevapi.Schema
import com.mysql.cj.xdevapi.SessionFactory
import com.projectronin.interop.mock.ehr.xdevapi.SafeXDev.SafeCollection
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Resource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SafeXDevTest {
    private val schema = mockk<Schema>()
    private val config = XDevConfig("host", "8080", "schema", "user", "pass")
    private lateinit var xdev: SafeXDev

    @BeforeEach
    fun setup() {
        mockkConstructor(SessionFactory::class)

        every {
            anyConstructed<SessionFactory>().getSession("mysqlx://host:8080/schema?user=user&password=pass").defaultSchema
        } returns schema

        xdev = SafeXDev(config)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createCollection uses schema when not cached`() {
        val xdevCollection = mockk<Collection>(relaxed = true)
        every { schema.createCollection("Patient", true) } returns xdevCollection

        val collection = xdev.createCollection(Patient::class.java)
        assertNotNull(collection)

        xdev.run(collection) {
            assertEquals(xdevCollection, this)
        }
    }

    @Test
    fun `createCollection uses cache when cached`() {
        val collection = mockk<SafeCollection>(relaxed = true)

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
            mockk(relaxed = true)
        }
        every { schema.createCollection("Practitioner", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            time2 = System.currentTimeMillis()
            mockk(relaxed = true)
        }

        runBlocking {
            awaitAll(
                async {
                    xdev.createCollection(Patient::class.java)
                },
                async {
                    xdev.createCollection(Practitioner::class.java)
                },
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
            mockk(relaxed = true)
        }
        every { schema.createCollection("Practitioner", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            mockk(relaxed = true)
        }

        val values =
            runBlocking {
                awaitAll(
                    async {
                        xdev.createCollection(Patient::class.java)
                        System.currentTimeMillis()
                    },
                    async {
                        xdev.createCollection(Practitioner::class.java)
                        System.currentTimeMillis()
                    },
                )
            }

        val diff = abs(values.first() - values.last())
        // A bit of leeway here, but this should basically be 0
        assertTrue(diff < 100)
    }

    @Test
    fun `createCollection and run use same mutex`() {
        val delayTimeInSeconds = 5

        var time1: Long = 0
        every { schema.createCollection("Patient", true) } answers {
            runBlocking {
                delay(delayTimeInSeconds.toDuration(DurationUnit.SECONDS))
            }
            time1 = System.currentTimeMillis()
            mockk(relaxed = true)
        }

        val collection2 = SafeCollection("name", mockk(relaxed = true))

        var time2: Long = 0
        val task = {
            xdev.run(collection2) {
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
                },
            )
        }

        val diff = abs(time1 - time2)
        // If we blocked, then one task will finish at least [delayTimeInSeconds] seconds after the other, but measured in milliseconds.
        assertTrue(time1 != 0L && diff >= (delayTimeInSeconds * 1000))
    }

    @Test
    fun `run resets failed collections`() {
        val schema2 = mockk<Schema>()
        every { anyConstructed<SessionFactory>().getSession("mysqlx://host:8080/schema?user=user&password=pass").defaultSchema } returnsMany
            listOf(
                schema,
                schema2,
            )

        xdev = SafeXDev(config)

        val patientCollection1 = mockk<Collection>(relaxed = true)
        every { schema.createCollection("Patient", true) } returns patientCollection1
        val patientCollection2 = mockk<Collection>(relaxed = true)
        every { schema2.createCollection("Patient", true) } returns patientCollection2

        val locationCollection1 =
            mockk<Collection> {
                every { count() } throws CJCommunicationsException()
            }
        every { schema.createCollection("Location", true) } returns locationCollection1
        val locationCollection2 = mockk<Collection>(relaxed = true)
        every { schema2.createCollection("Location", true) } returns locationCollection2

        val patientSafeCollection = xdev.createCollection(Patient::class.java)
        assertNotNull(patientSafeCollection)
        assertEquals(patientCollection1, patientSafeCollection.collection)

        val locationSafeCollection = xdev.createCollection(Location::class.java)
        assertNotNull(locationSafeCollection)
        assertEquals(locationCollection1, locationSafeCollection.collection)

        xdev.run(locationSafeCollection) {
            assertEquals(locationCollection2, this)
        }

        // Both of these connections should be reset behind the scenes
        assertEquals(patientCollection2, patientSafeCollection.collection)
        assertEquals(locationCollection2, locationSafeCollection.collection)

        verify(exactly = 1) { locationCollection1.count() }
        verify(exactly = 1) { schema.createCollection("Patient", true) }
        verify(exactly = 1) { schema2.createCollection("Patient", true) }
        verify(exactly = 1) { schema.createCollection("Location", true) }
        verify(exactly = 1) { schema2.createCollection("Location", true) }
    }
}
