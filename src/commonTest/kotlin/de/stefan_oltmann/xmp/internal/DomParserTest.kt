package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XMPMetaFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the corrupted document fallbacks of [DomParser].
 */
class DomParserTest {

    /**
     * Junk before the RDF part is cut off.
     */
    @Test
    fun testParseWithJunkBeforeRdf() {

        val testXmp = """
            random junk
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getObjectName())
    }

    /**
     * Junk after the packet is cut off.
     */
    @Test
    fun testParseWithJunkAfterPacket() {

        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:title>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Titel</rdf:li>
                    </rdf:Alt>
                  </dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
            trailing junk
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("Titel", xmpMeta.getTitle())
    }

    /**
     * NUL bytes before the packet are cut off.
     */
    @Test
    fun testParseWithNulBytesBeforePacket() {

        val testXmp = "\u0000\u0000junk\u0000" + """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""/>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertTrue(checkNotNull(xmpMeta.getPacketHeader()).contains("W5M0MpCehiHzreSzNTczkc9d"))
    }

    /**
     * A single self-closing rdf:RDF tag inside junk is recovered.
     */
    @Test
    fun testParseWithSelfClosingRdfTag() {

        val testXmp = """
            garbage <rdf:RDF xmlns:rdf="${XMPConst.NS_RDF}"/> garbage
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getObjectName())
    }

    /**
     * Junk before the RDF part that breaks the first fallback is trimmed to
     * the RDF part.
     */
    @Test
    fun testParseWithUnparseablePrefixIsTrimmedToRdf() {

        val testXmp = """
            junk <garbage/>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getObjectName())
    }

    /**
     * A blank document is rejected.
     */
    @Test
    fun testParseBlankDocumentThrows() {

        val ex = assertFailsWith<XMPException> {
            DomParser.parseDocumentFromString("   ")
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * A document that cannot be recovered is rejected.
     */
    @Test
    fun testParseUnrecoverableDocumentThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString("this is not xml <broken")
        }

        assertEquals(XMPErrorConst.BADSTREAM, ex.errorCode)
    }
}
