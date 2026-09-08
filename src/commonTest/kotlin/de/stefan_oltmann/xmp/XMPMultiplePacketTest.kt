package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests which packet wins when the input contains more than one complete XMP packet, like
 * containers that concatenated several packets. The first complete packet must win and the
 * remaining data must be ignored, exactly like ExifTool does it.
 */
class XMPMultiplePacketTest {

    /**
     * When both packets are wrapped into `<?xpacket?>` processing instructions and
     * `x:xmpmeta` elements, the first packet is parsed and the second is ignored.
     */
    @Test
    fun testFirstOfTwoWrappedPacketsWins() {

        val input = wrappedPacket(first = true) + "\n" + wrappedPacket(first = false)

        val xmpMeta = XMPMetaFactory.parseFromString(input)

        assertFirstPacket(xmpMeta)
    }

    /**
     * When both packets are bare `rdf:RDF` documents, the first one wins.
     */
    @Test
    fun testFirstOfTwoBarePacketsWins() {

        val input = barePacket(first = true) + "\n" + barePacket(first = false)

        val xmpMeta = XMPMetaFactory.parseFromString(input)

        assertFirstPacket(xmpMeta)
    }

    /**
     * A wrapped packet followed by a bare packet, and the other way around, both deliver the
     * first packet.
     */
    @Test
    fun testFirstOfMixedPacketsWins() {

        val wrappedFirst = XMPMetaFactory.parseFromString(
            wrappedPacket(first = true) + "\n" + barePacket(first = false)
        )

        assertFirstPacket(wrappedFirst)

        val bareFirst = XMPMetaFactory.parseFromString(
            barePacket(first = true) + "\n" + wrappedPacket(first = false)
        )

        assertFirstPacket(bareFirst)
    }

    /**
     * Junk before the packets, like the NUL padding of corrupted files, does not change which
     * packet wins.
     */
    @Test
    fun testFirstPacketWinsWithJunkBefore() {

        val input = "\u0000\u0000junk before the packet\n" +
            wrappedPacket(first = true) + "\n" + barePacket(first = false)

        val xmpMeta = XMPMetaFactory.parseFromString(input)

        assertFirstPacket(xmpMeta)
    }

    /**
     * Builds a complete packet wrapped into `<?xpacket?>` and `x:xmpmeta`.
     */
    private fun wrappedPacket(first: Boolean): String = """
        <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
        <x:xmpmeta xmlns:x="adobe:ns:meta/">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:xmp="http://ns.adobe.com/xap/1.0/"
              xmp:packetMarker="${if (first) "first" else "second"}">
              <dc:title>${if (first) "First title" else "Second title"}</dc:title>
            </rdf:Description>
          </rdf:RDF>
        </x:xmpmeta>
        <?xpacket end="w"?>
    """.trimIndent()

    /**
     * Builds a packet as a bare `rdf:RDF` document.
     */
    private fun barePacket(first: Boolean): String = """
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
          <rdf:Description rdf:about=""
              xmlns:dc="http://purl.org/dc/elements/1.1/"
              xmlns:xmp="http://ns.adobe.com/xap/1.0/"
            xmp:packetMarker="${if (first) "first" else "second"}">
            <dc:title>${if (first) "First title" else "Second title"}</dc:title>
          </rdf:Description>
        </rdf:RDF>
    """.trimIndent()

    /**
     * Verifies that the parsed metadata comes from the first packet: its marker value and
     * title are present, the second packet's marker value is not.
     */
    private fun assertFirstPacket(xmpMeta: XMPMeta) {

        assertEquals("first", xmpMeta.getPropertyString(XMPConst.NS_XMP, "packetMarker"))

        assertEquals("First title", xmpMeta.getTitle())

        assertFalse(xmpMeta.getPropertyString(XMPConst.NS_XMP, "packetMarker") == "second")
    }
}
