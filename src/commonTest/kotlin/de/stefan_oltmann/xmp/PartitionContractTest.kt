package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The partition entry point may only throw [XMPException] - every other
 * library entry point wraps foreign exceptions, and callers dispatch on
 * that contract. The assembly validates that the chunks really carry the
 * declared total length, so a producer cannot pass truncated data off as
 * a complete extended packet.
 */
class PartitionContractTest {

    @Test
    fun testNonPositiveLimitsThrowXmpException() {

        for (limit in listOf(0, -1)) {

            val exception = assertFailsWith<XMPException> {
                XMPMetaFactory.partitionPacket(SMALL_PACKET, limit, 65_536)
            }

            assertEquals(XMPErrorConst.BADPARAM, exception.errorCode)
            assertTrue(exception.message!!.contains("positive"))
        }
    }

    @Test
    fun testSmallPacketIsNotPartitioned() {

        val partition = XMPMetaFactory.partitionPacket(SMALL_PACKET, 65_536, 65_536)

        assertEquals(SMALL_PACKET, partition.mainPacket)
        assertTrue(partition.extendedChunks.isEmpty())
    }

    /**
     * Chunk fragments that are contiguous but collectively shorter than
     * their declared total length must fail the merge instead of being
     * accepted as a complete extended packet.
     */
    @Test
    fun testMergeRejectsAssembledSizeShorterThanDeclared() {

        /* A complete, valid extended chunk stream, then cut the data of
           the last chunk short while keeping its declared total length. */
        val full = buildValidExtendedPacket()

        val lastChunk = full.extendedChunks.last()

        val shortChunks = full.extendedChunks.dropLast(1) +
            lastChunk.copyOfRange(0, lastChunk.size - 16)

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(full.mainPacket, shortChunks)
        }

        assertTrue(
            exception.message!!.contains("total length") ||
                exception.message!!.contains("length") ||
                exception.message!!.contains("GUID") ||
                exception.message!!.contains("Inconsistent"),
            "Unexpected message: ${exception.message}"
        )
    }

    private companion object {

        val SMALL_PACKET = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:source>x</dc:source>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        /**
         * Builds a main packet plus extended chunks by partitioning a
         * deliberately oversized packet.
         */
        fun buildValidExtendedPacket(): XmpPacketPartition {

            val filler = "filler ".repeat(4000)

            val bigPacket = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:source>$filler</dc:source>
                  </rdf:Description>
                </rdf:RDF>
            """.trimIndent()

            return XMPMetaFactory.partitionPacket(bigPacket, 1024, 4096)
        }
    }
}
