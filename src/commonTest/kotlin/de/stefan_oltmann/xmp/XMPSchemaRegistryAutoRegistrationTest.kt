package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Parsing registers unknown namespaces automatically in the process-global registry. A single
 * hostile document must not be able to introduce unlimited namespaces, so the automatic
 * registrations are capped per document while explicit client registrations stay unlimited.
 * Unlike a lifetime-global cap this never fails valid documents because of what other
 * documents parsed before them.
 */
class XMPSchemaRegistryAutoRegistrationTest {

    /**
     * Builds a document that uses [count] distinct namespaces, each for one child element.
     */
    private fun buildManyNamespaceDocument(count: Int, uriBase: String): String {

        val open = StringBuilder()

        val close = StringBuilder()

        open.append(
            "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>" +
                "<rdf:Description rdf:about=''"
        )

        for (index in 1..count) {
            open.append(" xmlns:n$index=\"$uriBase$index/\"")
            close.insert(0, "</n$index:p$index>")
        }

        open.append(">")

        for (index in 1..count)
            open.append("<n$index:p$index>x</n$index:p$index>")

        open.append("</rdf:Description></rdf:RDF>").append(close)

        return open.toString()
    }

    /**
     * A document with several unknown namespaces still parses and its properties stay
     * accessible afterwards.
     */
    @Test
    fun testParsingUnknownNamespacesStillWorks() {

        val baseUri = nextBaseUri("ok")

        /* language=XML */
        val input = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:ns1="$baseUri"
                  xmlns:ns2="$baseUri second/">
                <ns1:prop>value</ns1:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(input)

        assertEquals("value", xmpMeta.getPropertyString(baseUri, "ns1:prop"))
    }

    /**
     * A single document that introduces more than the allowed number of namespaces fails with
     * BADSCHEMA, while parsing further documents still works afterwards - the cap is per
     * document, not lifetime-global.
     */
    @Test
    fun testExcessiveNamespacesInOneDocumentIsRejected() {

        val uriBase = nextBaseUri("cap")

        val created = mutableListOf<String>()

        try {

            val count = XMPMeta.MAX_AUTO_REGISTERED_NAMESPACES_PER_DOCUMENT + 100

            val ex = assertFailsWith<XMPException> {
                XMPMetaFactory.parseFromString(buildManyNamespaceDocument(count, uriBase))
            }

            assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)

            /*
             * The rejected document may have registered namespaces up to the cap before
             * failing. Free them so the process-global registry stays clean.
             */
            for (index in 1..XMPMeta.MAX_AUTO_REGISTERED_NAMESPACES_PER_DOCUMENT)
                created.add("$uriBase$index/")

            /*
             * A completely unrelated document with new unknown namespaces still parses,
             * proving nothing is poisoned for subsequent parses.
             */
            val followUpBaseUri = nextBaseUri("followup")

            /* language=XML */
            val followUp = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="" xmlns:later="$followUpBaseUri">
                    <later:prop>fine</later:prop>
                  </rdf:Description>
                </rdf:RDF>
            """.trimIndent()

            assertEquals(
                "fine",
                XMPMetaFactory.parseFromString(followUp).getPropertyString(followUpBaseUri, "later:prop")
            )

            created.add("$followUpBaseUri")

            /* Explicit registrations are not capped. */
            val explicitNamespace = "${nextBaseUri("explicit")}explicit/"

            XMPSchemaRegistry.registerNamespace(explicitNamespace, "capExplicit")

            assertEquals(
                "capExplicit:",
                XMPSchemaRegistry.getNamespacePrefix(explicitNamespace)
            )

            created.add("$explicitNamespace")

        } finally {

            /* Free the registry budget again so other tests are not affected. */
            for (namespaceURI in created)
                XMPSchemaRegistry.deleteNamespace(namespaceURI)
        }
    }

    companion object {

        /*
         * The registry lives for the whole test process, so every run needs URIs that no other
         * test has used before. A plain counter is common-code and unique within the process,
         * unlike JVM-only timestamps.
         */
        private var uriCounter = 0

        /**
         * Returns a namespace URI prefix that is unique within the test process.
         */
        private fun nextBaseUri(suffix: String): String {
            uriCounter++

            return "http://example.org/xmpcore-autocap-$suffix-${uriCounter}/"
        }
    }
}
