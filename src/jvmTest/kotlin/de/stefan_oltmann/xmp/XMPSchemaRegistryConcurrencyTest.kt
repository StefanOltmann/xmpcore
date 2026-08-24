package de.stefan_oltmann.xmp

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for the thread safety of [XMPSchemaRegistry].
 *
 * The registry is a process-global singleton that parsing mutates whenever a file contains an
 * unknown namespace. Concurrent registrations must never lose updates, generate conflicting
 * prefixes or leave the prefix and namespace lookups inconsistent.
 */
class XMPSchemaRegistryConcurrencyTest {

    /**
     * Many threads register private namespaces and race on shared namespaces with the same
     * suggested prefix. Afterwards every namespace must exist exactly once, every prefix must
     * resolve back to its namespace and no two namespaces may share a prefix.
     */
    @Test
    fun testConcurrentRegistrationsStayConsistent() {

        /* Unique base URI so repeated runs in the same JVM cannot interfere. */
        val baseUri = "http://example.org/xmpcore-concurrency-${System.nanoTime()}/"

        val threadCount = 8
        val namespacesPerThread = 25

        /* Namespaces raced on by every thread with the same suggested prefix. */
        val sharedUris = (1..10).map { index -> "${baseUri}shared/$index/" }

        val expectedNamespaceCount = threadCount * namespacesPerThread + sharedUris.size

        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        val pool = Executors.newFixedThreadPool(threadCount)

        try {

            repeat(threadCount) { threadIndex ->

                pool.execute {

                    try {

                        start.await()

                        for (index in 1..namespacesPerThread)
                            XMPSchemaRegistry.registerNamespace(
                                "${baseUri}thread$threadIndex/$index/",
                                "p$threadIndex"
                            )

                        for (uri in sharedUris)
                            XMPSchemaRegistry.registerNamespace(uri, "shared")

                    } finally {

                        done.countDown()
                    }
                }
            }

            start.countDown()

            assertTrue(done.await(30, TimeUnit.SECONDS), "Threads did not finish in time")

            /* Assert: all registrations survived, lookups are consistent and bijective. */

            val registered = XMPSchemaRegistry.getNamespaces().filterKeys { it.startsWith(baseUri) }

            assertEquals(
                expected = expectedNamespaceCount,
                actual = registered.size,
                message = "Every concurrently registered namespace must survive"
            )

            for ((namespace, prefix) in registered)
                assertEquals(
                    expected = namespace,
                    actual = XMPSchemaRegistry.getNamespaceURI(prefix),
                    message = "Prefix '$prefix' must resolve back to its own namespace"
                )

            assertEquals(
                expected = registered.size,
                actual = registered.values.toSet().size,
                message = "No two namespaces may share a prefix"
            )

        } finally {

            pool.shutdownNow()
        }
    }
}
