package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the Adobe extended XMP support of [XMPMetaFactory]: oversized packets are partitioned
 * into a main packet plus chunks and merged back, with validation of the Adobe chunk format.
 * The used size limits are far below the real JPEG segment limits, so a handful of properties
 * is enough to force a split.
 */
class XMPMetaExtendedXmpTest {

    private val maxMainPacketBytes = 600

    private val maxExtendedChunkBytes = 200

    /**
     * The registered schema namespaces of the test packet, so the serializer writes one
     * `rdf:Description` block per schema and the partitioner can keep some and move others.
     */
    private val schemaNamespaces = listOf(
        XMPConst.NS_DC,
        XMPConst.NS_XMP,
        XMPConst.NS_EXIF,
        XMPConst.NS_TIFF,
        XMPConst.NS_PHOTOSHOP,
        XMPConst.NS_IPTC_CORE
    )

    /**
     * A packet that fits into the size limit needs no extended chunks and survives unchanged
     * apart from the removed in-place editing padding.
     */
    @Test
    fun testPartitionSmallPacketNeedsNoChunks() {

        val packet = createPacket(schemaCount = 1, propertiesPerSchema = 2)

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.isEmpty())
        assertFalse(partition.mainPacket.contains("HasExtendedXMP"))
    }

    /**
     * An oversized packet is split so the main packet respects the size limit, references the
     * extended data and every property survives the round trip through the chunks.
     */
    @Test
    fun testPartitionOversizedPacketRoundTrip() {

        val packet = createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5)

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.isNotEmpty())

        assertTrue(partition.mainPacket.contains("xmpNote:HasExtendedXMP"))

        assertTrue(
            partition.mainPacket.encodeToByteArray().size <= maxMainPacketBytes,
            "Main packet must respect the size limit"
        )

        val merged = XMPMetaFactory.assemblePacket(partition.mainPacket, partition.extendedChunks)

        val xmpMeta = XMPMetaFactory.parseFromString(merged)

        assertAllPropertiesPresent(xmpMeta, schemaNamespaces.size, 5)
    }

    /**
     * Parsing the partitioned packet and its chunks directly delivers the complete data
     * model, and the consumed reference is not part of the tree.
     */
    @Test
    fun testParseFromExtendedChunks() {

        val packet = createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5)

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val xmpMeta = XMPMetaFactory.parseFromString(partition.mainPacket, partition.extendedChunks)

        assertAllPropertiesPresent(xmpMeta, schemaNamespaces.size, 5)

        assertFalse(
            xmpMeta.doesPropertyExist(XMPConst.NS_XMP_NOTE, "HasExtendedXMP"),
            "The consumed reference must not stay in the parsed tree"
        )
    }

    /**
     * The chunks follow Adobe's extended XMP chunk format: signature, the GUID of the main
     * packet's reference, the big-endian total length, the big-endian offset of the chunk and
     * the data itself.
     */
    @Test
    fun testChunkLayoutMatchesAdobeFormat() {

        val packet = createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5)

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val guid = findReferenceGuid(partition.mainPacket)

        val signature = "http://ns.adobe.com/xmp/extension/\u0000".encodeToByteArray()

        var expectedOffset = 0

        for (chunk in partition.extendedChunks) {

            assertTrue(chunk.size > signature.size + 32 + 8)

            assertContentEquals(signature, chunk.copyOfRange(0, signature.size))

            assertEquals(
                expected = guid,
                actual = chunk.decodeToString(signature.size, signature.size + 32)
            )

            val totalLength = readIntBE(chunk, signature.size + 32)

            val offset = readIntBE(chunk, signature.size + 36)

            assertEquals(expectedOffset, offset)

            assertEquals(partition.extendedChunks.first().let(::readTotalLength), totalLength)

            assertTrue(chunk.size - signature.size - 40 <= maxExtendedChunkBytes)

            expectedOffset += chunk.size - signature.size - 40
        }

        assertEquals(readTotalLength(partition.extendedChunks.first()), expectedOffset)
    }

    /**
     * Stale `xmpNote:HasExtendedXMP` references of a previous write, in attribute or element
     * form, are removed, because they would point at chunks that are never emitted.
     */
    @Test
    fun testPartitionRemovesStaleReferences() {

        val guid = "0123456789ABCDEF0123456789ABCDEF"

        val packet = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
                  xmpNote:HasExtendedXMP="$guid">
                <dc:title>Titel</dc:title>
              </rdf:Description>
              <rdf:Description rdf:about="" xmlns:xmpNote="http://ns.adobe.com/xmp/note/">
                <xmpNote:HasExtendedXMP>$guid</xmpNote:HasExtendedXMP>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.isEmpty())

        assertFalse(partition.mainPacket.contains("HasExtendedXMP"))
    }

    /**
     * In-place editing padding carries no information and must not push an otherwise small
     * packet over the size limit.
     */
    @Test
    fun testPartitionStripsPaddingBeforeMeasuring() {

        val smallPacket = createPacket(schemaCount = 1, propertiesPerSchema = 2)

        val paddedPacket = smallPacket.replace(
            "<?xpacket end=",
            " ".repeat(maxMainPacketBytes) + "<?xpacket end="
        )

        assertTrue(paddedPacket.encodeToByteArray().size > maxMainPacketBytes)

        val partition = XMPMetaFactory.partitionPacket(
            paddedPacket,
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.isEmpty())

        assertFalse(partition.mainPacket.contains(" ".repeat(10)))
    }

    /**
     * A packet whose RDF root contains elements other than `rdf:Description` cannot be
     * partitioned without losing data and fails the write instead. The tiny size limit makes
     * the small test packet oversized, so the splitting path is reached.
     */
    @Test
    fun testPartitionRejectsUnrecognizedRdfContent() {

        val packet = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <foo:Bar xmlns:foo="http://example.org/foo/"/>
            </rdf:RDF>
        """.trimIndent()

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.partitionPacket(packet, maxMainPacketBytes = 50, maxExtendedChunkBytes = 40)
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * Size limits that cannot hold any content are rejected as programming errors.
     */
    @Test
    fun testPartitionRejectsInvalidLimits() {

        val packet = createPacket(schemaCount = 1, propertiesPerSchema = 1)

        assertFailsWith<IllegalArgumentException> {
            XMPMetaFactory.partitionPacket(packet, 0, maxExtendedChunkBytes)
        }

        assertFailsWith<IllegalArgumentException> {
            XMPMetaFactory.partitionPacket(packet, maxMainPacketBytes, 0)
        }
    }

    /**
     * A packet without a reference is returned unchanged and the chunks are ignored.
     */
    @Test
    fun testAssembleWithoutReferenceIgnoresChunks() {

        val packet = createPacket(schemaCount = 1, propertiesPerSchema = 2)

        val merged = XMPMetaFactory.assemblePacket(packet, listOf(byteArrayOf(1, 2, 3)))

        assertEquals(packet, merged)
    }

    /**
     * A reference without any chunks is a corrupted file and fails loudly instead of
     * silently dropping the extended metadata.
     */
    @Test
    fun testAssembleWithReferenceButNoChunksThrows() {

        val mainPacket = createMainPacketWithReference("0123456789ABCDEF0123456789ABCDEF")

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(mainPacket, emptyList())
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * Chunks of a foreign GUID belong to another packet; when none of the chunks matches the
     * reference the file is corrupted and fails.
     */
    @Test
    fun testAssembleWithForeignGuidOnlyThrows() {

        val partitionA = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val partitionB = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 4),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(partitionA.mainPacket, partitionB.extendedChunks)
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * Corrupted chunk data is detected through the MD5 GUID check.
     */
    @Test
    fun testAssembleWithCorruptedChunkDataThrows() {

        val partition = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val lastChunk = partition.extendedChunks.last()

        val corrupted = lastChunk.copyOf()

        corrupted[corrupted.size - 1] = (corrupted[corrupted.size - 1] + 1).toByte()

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(partition.mainPacket, partition.extendedChunks.dropLast(1) + corrupted)
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * A gap between the chunk offsets means a chunk is missing; the merge must fail instead
     * of shifting data silently. The chunks are handed over in scrambled order on purpose,
     * because the API sorts them by offset itself.
     */
    @Test
    fun testAssembleWithNonContiguousChunksThrows() {

        val partition = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.size >= 3)

        val withGap = listOf(
            partition.extendedChunks.first(),
            partition.extendedChunks.last()
        )

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(partition.mainPacket, withGap.asReversed())
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * Chunks that disagree about the total length of the extended data are rejected.
     */
    @Test
    fun testAssembleWithInconsistentTotalLengthThrows() {

        val partition = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        assertTrue(partition.extendedChunks.size >= 2)

        val tampered = partition.extendedChunks.first().copyOf()

        tampered[67] = (tampered[67] + 1).toByte()

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(partition.mainPacket, listOf(tampered) + partition.extendedChunks.drop(1))
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * A chunk that is too short to carry the full header is rejected.
     */
    @Test
    fun testAssembleWithTruncatedChunkThrows() {

        val mainPacket = createMainPacketWithReference("0123456789ABCDEF0123456789ABCDEF")

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.assemblePacket(mainPacket, listOf(ByteArray(10)))
        }

        assertEquals(XMPErrorConst.BADXMP, exception.errorCode)
    }

    /**
     * The assembled packet contains no trace of the consumed reference, so writing it back
     * cannot produce a stale pointer.
     */
    @Test
    fun testAssembleDropsConsumedReference() {

        val partition = XMPMetaFactory.partitionPacket(
            createPacket(schemaCount = schemaNamespaces.size, propertiesPerSchema = 5),
            maxMainPacketBytes,
            maxExtendedChunkBytes
        )

        val merged = XMPMetaFactory.assemblePacket(partition.mainPacket, partition.extendedChunks)

        assertFalse(merged.contains("HasExtendedXMP"))
    }

    /**
     * Builds a packet with the given number of schema blocks and properties per schema.
     */
    private fun createPacket(schemaCount: Int, propertiesPerSchema: Int): String {

        val xmpMeta = XMPMetaFactory.create()

        for (schemaIndex in 0 until schemaCount)
            for (propertyIndex in 0 until propertiesPerSchema)
                xmpMeta.setProperty(
                    schemaNamespaces[schemaIndex],
                    "testProperty$propertyIndex",
                    "value-$schemaIndex-$propertyIndex-with-some-length"
                )

        return XMPMetaFactory.serializeToString(xmpMeta, SerializeOptions())
    }

    /**
     * Builds a main packet that references extended data through the element form.
     */
    private fun createMainPacketWithReference(guid: String): String = """
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
          <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>Titel</dc:title>
          </rdf:Description>
          <rdf:Description rdf:about="" xmlns:xmpNote="http://ns.adobe.com/xmp/note/">
            <xmpNote:HasExtendedXMP>$guid</xmpNote:HasExtendedXMP>
          </rdf:Description>
        </rdf:RDF>
    """.trimIndent()

    /**
     * Verifies that every property of the test packet is readable with its original value.
     */
    private fun assertAllPropertiesPresent(xmpMeta: XMPMeta, schemaCount: Int, propertiesPerSchema: Int) {

        for (schemaIndex in 0 until schemaCount) {

            val namespace = schemaNamespaces[schemaIndex]

            for (propertyIndex in 0 until propertiesPerSchema) {

                assertEquals(
                    expected = "value-$schemaIndex-$propertyIndex-with-some-length",
                    actual = xmpMeta.getPropertyString(namespace, "testProperty$propertyIndex"),
                    message = "schema $schemaIndex, property $propertyIndex"
                )
            }
        }
    }

    /**
     * Extracts the GUID of the `xmpNote:HasExtendedXMP` reference of the main packet.
     */
    private fun findReferenceGuid(mainPacket: String): String {

        val match = Regex("<xmpNote:HasExtendedXMP>([0-9A-Fa-f]{32})</xmpNote:HasExtendedXMP>")
            .find(mainPacket)

        return requireNotNull(match?.groupValues?.get(1)) { "No reference found in main packet" }
    }

    /**
     * Reads the big-endian total length field of a chunk.
     */
    private fun readTotalLength(chunk: ByteArray): Int = readIntBE(chunk, 67)

    /**
     * Reads a big-endian int at the given offset.
     */
    private fun readIntBE(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
}
