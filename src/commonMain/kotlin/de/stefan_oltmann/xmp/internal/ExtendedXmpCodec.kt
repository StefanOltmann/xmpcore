package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XmpPacketPartition

/**
 * Implements the Adobe extended XMP scheme: oversized XMP packets are split into a main
 * packet that keeps as many whole `rdf:Description` blocks as fit into a size limit and
 * references the remaining data through `xmpNote:HasExtendedXMP`; the moved blocks are
 * serialized into chunks that carry the MD5 GUID of the extended data, its total length and
 * one slice of the data each. This is the mechanism specified by Adobe (XMP specification)
 * and is read by ExifTool, Photoshop and Lightroom.
 */
@Suppress("TooManyFunctions")
internal object ExtendedXmpCodec {

    /**
     * The signature every extended XMP chunk starts with.
     */
    private const val CHUNK_SIGNATURE = "http://ns.adobe.com/xmp/extension/\u0000"

    /**
     * The GUID of an extended XMP packet is a 32-character hexadecimal string.
     */
    private const val GUID_LENGTH = 32

    private const val TOTAL_LENGTH_BYTES = 4

    private const val OFFSET_BYTES = 4

    /**
     * Byte length of the chunk header: signature, GUID, total length and offset.
     */
    private val CHUNK_HEADER_BYTES =
        CHUNK_SIGNATURE.encodeToByteArray().size + GUID_LENGTH + TOTAL_LENGTH_BYTES + OFFSET_BYTES

    private const val RDF_OPEN_TAG = "<rdf:RDF"

    private const val RDF_CLOSE_TAG = "</rdf:RDF>"

    private const val DESCRIPTION_OPEN_TAG = "<rdf:Description"

    /**
     * A stale `xmpNote:HasExtendedXMP` reference from a previous write must not survive:
     * the reference is regenerated whenever extended data is written, like ExifTool does it,
     * which deletes the tag because "we create it as needed".
     */
    private const val STALE_REFERENCE_MARKER = "xmpNote:HasExtendedXMP"

    /**
     * Minimal self-contained wrapper for the extended data. The moved `rdf:Description`
     * blocks declare their own namespaces, so no further declarations are required here.
     */
    private const val MINIMAL_HEADER =
        """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">"""

    private const val MINIMAL_FOOTER = "</rdf:RDF></x:xmpmeta>"

    private const val PACKET_END_MARKER = "<?xpacket end="

    /*
     * The attribute and element serialization forms of the reference, both for reading the
     * GUID of an existing packet and for removing stale references before writing.
     */
    private val attributeReferenceRegex =
        Regex("""xmpNote:HasExtendedXMP\s*=\s*["']([0-9A-Fa-f]{32})["']""")

    private val elementReferenceRegex =
        Regex("""<xmpNote:HasExtendedXMP>\s*([0-9A-Fa-f]{32})\s*</xmpNote:HasExtendedXMP>""")

    private val staleAttributeReferenceRegex = Regex("""xmpNote:HasExtendedXMP\s*=\s*["'][^"']*["']""")

    private val staleElementReferenceRegex =
        Regex("""<xmpNote:HasExtendedXMP>\s*[0-9A-Fa-f]{32}\s*</xmpNote:HasExtendedXMP>""")

    /**
     * Splits the given packet when it exceeds [maxMainPacketBytes]. The packet terminator
     * always starts its own line in the returned main packet. Packets that fit are returned
     * without extended chunks and keep their original bytes, so a container rewrite stays
     * byte-identical apart from a removed stale reference, which would point at chunks that
     * are never emitted. Only a packet that exceeds the limit has its in-place editing
     * padding collapsed into the canonical line break, because the padding would distort
     * the size measurement.
     *
     * @param packet the serialized XMP packet to split.
     * @param maxMainPacketBytes the maximum UTF-8 byte size of the main packet.
     * @param maxExtendedChunkBytes the maximum number of extended data bytes per chunk; the
     * chunk header on top of it must fit into the container's storage unit as well.
     * @return Returns the main packet and the extended chunks, in order.
     * @throws XMPException if the packet exceeds the limit but uses RDF structures the
     * splitter cannot partition.
     */
    fun partition(
        packet: String,
        maxMainPacketBytes: Int,
        maxExtendedChunkBytes: Int
    ): XmpPacketPartition {

        require(maxMainPacketBytes > 0) { "Max main packet bytes must be positive: $maxMainPacketBytes" }

        require(maxExtendedChunkBytes > 0) { "Max extended chunk bytes must be positive: $maxExtendedChunkBytes" }

        val withoutStaleReference = removeStaleExtendedXmpReference(packet)

        val canonicalPacket = ensureTerminatorOnOwnLine(withoutStaleReference)

        if (canonicalPacket.encodeToByteArray().size <= maxMainPacketBytes)
            return XmpPacketPartition(canonicalPacket, emptyList())

        val strippedPacket = stripPadding(withoutStaleReference)

        if (strippedPacket.encodeToByteArray().size <= maxMainPacketBytes)
            return XmpPacketPartition(strippedPacket, emptyList())

        return splitPacket(strippedPacket, maxMainPacketBytes, maxExtendedChunkBytes)
    }

    /**
     * Merges a main packet that references extended XMP data with its chunks into one packet,
     * validating that the chunks are complete, contiguous and match the declared MD5 GUID.
     * The consumed reference is removed from the result, because it would point at extension
     * chunks that no longer exist after a rewrite of the merged packet.
     *
     * A packet without a reference is returned unchanged and the chunks are ignored.
     *
     * @param mainPacket the serialized main packet.
     * @param extendedChunks the raw extended XMP chunks in Adobe's chunk format, in any order.
     * @return Returns the merged packet text, ready for [XMPMetaFactory.parseFromString].
     * @throws XMPException if the reference cannot be satisfied or the chunks are corrupt.
     */
    fun merge(mainPacket: String, extendedChunks: List<ByteArray>): String {

        val guid = findExtendedXmpGuid(mainPacket) ?: return mainPacket

        if (extendedChunks.isEmpty())
            throw XMPException(
                "The XMP packet references extended data (GUID $guid), but no extended XMP chunks exist.",
                XMPErrorConst.BADXMP
            )

        val fragments = extendedChunks.map { chunk -> decodeChunk(chunk) }

        /* Chunks of a foreign GUID belong to another packet and are ignored. */
        val matchingFragments = fragments.filter { fragment ->
            fragment.guid.equals(guid, ignoreCase = true)
        }

        if (matchingFragments.isEmpty())
            throw XMPException(
                "The XMP packet references extended data (GUID $guid), but no extended XMP chunk " +
                    "with this GUID exists.",
                XMPErrorConst.BADXMP
            )

        val extendedBytes = assembleChunks(guid, matchingFragments)

        val mergedPacket = injectExtendedDescriptions(
            mainPacket,
            extendedBytes.decodeToString(),
            guid
        )

        return removeStaleExtendedXmpReference(mergedPacket)
    }

    /**
     * Splits an oversized packet into its `rdf:Description` blocks and returns the partition
     * with the reference description in the main packet and the chunked extended data.
     */
    private fun splitPacket(
        cleanedPacket: String,
        maxMainPacketBytes: Int,
        maxExtendedChunkBytes: Int
    ): XmpPacketPartition {

        val headerEnd = locateTagEnd(cleanedPacket, RDF_OPEN_TAG)

        val footerStart = cleanedPacket.indexOf(RDF_CLOSE_TAG)

        if (headerEnd == -1 || footerStart == -1 || headerEnd > footerStart)
            throw XMPException(
                "The XMP packet has an unexpected structure and cannot be split into main and " +
                    "extended data.",
                XMPErrorConst.BADXMP
            )

        val header = cleanedPacket.substring(0, headerEnd)
        val footer = cleanedPacket.substring(footerStart)
        val content = cleanedPacket.substring(headerEnd, footerStart)

        /*
         * Content outside the recognized description blocks would be silently dropped from the
         * extended data. RDF legally allows node elements other than rdf:Description, so a
         * packet the splitter cannot fully understand must fail instead of losing metadata.
         * Whitespace between the blocks is not content.
         */
        val firstBlockStart = content.indexOf(DESCRIPTION_OPEN_TAG)

        val hasUnrecognizedContent =
            (firstBlockStart == -1 && content.isNotBlank()) ||
                (firstBlockStart > 0 && content.substring(0, firstBlockStart).isNotBlank())

        if (hasUnrecognizedContent)
            throw XMPException(
                "The XMP packet uses RDF structures this writer cannot split into main and " +
                    "extended data.",
                XMPErrorConst.BADXMP
            )

        val blocks = splitDescriptions(content)
            .filter { block -> !block.contains(STALE_REFERENCE_MARKER) }

        val (keptBlocks, movedBlocks) = selectBlocks(
            blocks,
            header,
            footer,
            maxMainPacketBytes
        )

        /*
         * Only the main packet must fit into the size limit. The extended data is chunked and
         * reassembled before parsing, so moving everything out is always possible. Readers
         * that ignore extended XMP see an almost empty main packet then, but they would
         * truncate or reject oversized metadata anyway.
         */
        if (movedBlocks.isEmpty())
            return XmpPacketPartition(header + blocks.joinToString("") + footer, emptyList())

        val extendedBytes = (MINIMAL_HEADER + movedBlocks.joinToString("") + MINIMAL_FOOTER)
            .encodeToByteArray()

        val guid = Md5.toHexString(Md5.digest(extendedBytes)).uppercase()

        val mainPacketXml = header + keptBlocks.joinToString("") +
            buildReferenceDescription(guid) + footer

        return XmpPacketPartition(
            mainPacketXml,
            createChunks(guid, extendedBytes, maxExtendedChunkBytes)
        )
    }

    /**
     * Selects the description blocks that stay in the main packet: like ExifTool, the
     * smallest blocks are kept first, so as much as possible remains readable by simple
     * tools. Returns the kept blocks and the blocks to move into the extended data.
     */
    private fun selectBlocks(
        blocks: List<String>,
        header: String,
        footer: String,
        maxMainPacketBytes: Int
    ): Pair<List<String>, List<String>> {

        val referenceDescriptionTemplate = buildReferenceDescription("0".repeat(GUID_LENGTH))

        val fixedMainBytes = (header + footer + referenceDescriptionTemplate).encodeToByteArray().size

        val sortedIndexes = blocks.withIndex()
            .sortedBy { (_, block) -> block.encodeToByteArray().size }

        var usedBytes = fixedMainBytes

        var keepCount = 0

        for ((_, block) in sortedIndexes) {

            val blockSize = block.encodeToByteArray().size

            if (usedBytes + blockSize > maxMainPacketBytes)
                break

            usedBytes += blockSize

            keepCount++
        }

        /* Selection works on indexes, so structurally identical blocks cannot alias. */
        val keptIndexes = sortedIndexes
            .take(keepCount)
            .map { (index, _) -> index }
            .toSet()

        val keptBlocks = sortedIndexes.take(keepCount).map { (_, block) -> block }

        val movedBlocks = blocks.filterIndexed { index, _ -> index !in keptIndexes }

        return keptBlocks to movedBlocks
    }

    /**
     * Builds the ready-to-embed chunks for the extended data: identifier, GUID, total length,
     * offset and one slice of the data each.
     */
    private fun createChunks(
        guid: String,
        extendedBytes: ByteArray,
        maxExtendedChunkBytes: Int
    ): List<ByteArray> {

        val signatureBytes = CHUNK_SIGNATURE.encodeToByteArray()
        val guidBytes = guid.encodeToByteArray()

        val chunks = mutableListOf<ByteArray>()

        var offset = 0

        do {

            val chunkEnd = minOf(offset + maxExtendedChunkBytes, extendedBytes.size)

            val dataSize = chunkEnd - offset

            val chunk = ByteArray(CHUNK_HEADER_BYTES + dataSize)

            signatureBytes.copyInto(chunk)
            guidBytes.copyInto(chunk, signatureBytes.size)

            writeIntBE(extendedBytes.size, chunk, signatureBytes.size + GUID_LENGTH)
            writeIntBE(offset, chunk, signatureBytes.size + GUID_LENGTH + TOTAL_LENGTH_BYTES)

            extendedBytes.copyInto(chunk, CHUNK_HEADER_BYTES, offset, chunkEnd)

            chunks.add(chunk)

            offset = chunkEnd
        } while (offset < extendedBytes.size)

        return chunks
    }

    /**
     * Decodes one chunk: signature, GUID, total length of the complete extended data, the
     * offset of this chunk inside that data and the data slice itself.
     */
    private fun decodeChunk(chunk: ByteArray): ExtendedXmpFragment {

        val signatureBytes = CHUNK_SIGNATURE.encodeToByteArray()

        if (chunk.size < CHUNK_HEADER_BYTES || !chunk.startsWith(signatureBytes))
            throw XMPException(
                "Invalid extended XMP chunk: wrong signature or truncated header.",
                XMPErrorConst.BADXMP
            )

        var index = signatureBytes.size

        val guidEnd = index + GUID_LENGTH

        val guid = chunk.decodeToString(index, guidEnd)

        index = guidEnd

        val totalLength = readIntBE(chunk, index)

        index += TOTAL_LENGTH_BYTES

        val offset = readIntBE(chunk, index)

        return ExtendedXmpFragment(guid, totalLength, offset, chunk.copyOfRange(CHUNK_HEADER_BYTES, chunk.size))
    }

    /**
     * Sorts the fragments by offset and verifies that the assembly is contiguous, complete
     * and matches the declared MD5 GUID, so missing or corrupted chunks fail loudly instead
     * of producing silently shifted or truncated metadata.
     */
    private fun assembleChunks(guid: String, fragments: List<ExtendedXmpFragment>): ByteArray {

        val declaredLength = fragments.first().totalLength

        val mismatchedLength = fragments.firstOrNull { fragment ->
            fragment.totalLength != declaredLength
        }

        if (mismatchedLength != null)
            throw XMPException(
                "Inconsistent extended XMP total length: ${mismatchedLength.totalLength} " +
                    "(expected $declaredLength).",
                XMPErrorConst.BADXMP
            )

        val sortedFragments = fragments.sortedBy { fragment -> fragment.offset }

        var expectedOffset = 0

        for (fragment in sortedFragments) {

            if (fragment.offset != expectedOffset)
                throw XMPException(
                    "The extended XMP chunks are not contiguous: expected offset " +
                        "$expectedOffset, got ${fragment.offset}.",
                    XMPErrorConst.BADXMP
                )

            expectedOffset += fragment.data.size
        }

        val extendedBytes = ByteArray(expectedOffset)

        var position = 0

        for (fragment in sortedFragments) {

            fragment.data.copyInto(extendedBytes, position)

            position += fragment.data.size
        }

        val actualDigest = Md5.toHexString(Md5.digest(extendedBytes))

        if (!actualDigest.equals(guid, ignoreCase = true))
            throw XMPException(
                "The MD5 checksum of the extended XMP data does not match the GUID $guid " +
                    "declared by the main packet.",
                XMPErrorConst.BADXMP
            )

        return extendedBytes
    }

    /**
     * Inserts the `rdf:Description` elements of the extended data into the main packet, so
     * both parse as one metadata tree afterwards.
     */
    private fun injectExtendedDescriptions(
        mainPacket: String,
        extendedXml: String,
        guid: String
    ): String {

        val innerStart = locateTagEnd(extendedXml, RDF_OPEN_TAG)

        val innerEnd = extendedXml.indexOf(RDF_CLOSE_TAG, innerStart)

        if (innerStart == -1 || innerEnd == -1)
            throw XMPException(
                "The extended XMP data referenced by GUID $guid has no RDF content.",
                XMPErrorConst.BADXMP
            )

        val descriptions = extendedXml.substring(innerStart, innerEnd)

        val insertionPoint = mainPacket.lastIndexOf(RDF_CLOSE_TAG)

        if (insertionPoint == -1)
            throw XMPException(
                "The main XMP packet referencing extended data (GUID $guid) has no RDF content.",
                XMPErrorConst.BADXMP
            )

        return mainPacket.substring(0, insertionPoint) + descriptions +
            mainPacket.substring(insertionPoint)
    }

    /**
     * Removes a stale `xmpNote:HasExtendedXMP` reference from the packet. Empty
     * `rdf:Description` wrappers of a removed reference are legal RDF and remain.
     */
    private fun removeStaleExtendedXmpReference(packet: String): String =
        packet
            .replace(staleAttributeReferenceRegex, "")
            .replace(staleElementReferenceRegex, "")

    /**
     * Adds the line break before the packet terminator processing instruction when the
     * terminator does not already start on its own line, so every returned packet keeps the
     * canonical layout. Whitespace that shares the content's line is replaced, while
     * in-place editing padding with line breaks of its own stays untouched.
     */
    private fun ensureTerminatorOnOwnLine(packet: String): String {

        val endIndex = packet.indexOf(PACKET_END_MARKER)

        if (endIndex == -1)
            return packet

        var runStart = endIndex

        while (runStart > 0 && packet[runStart - 1].isWhitespace())
            runStart--

        if (packet.indexOf('\n', runStart) in runStart until endIndex)
            return packet

        return packet.substring(0, runStart) + "\n" + packet.substring(endIndex)
    }

    /**
     * Collapses the in-place editing padding between the XMP content and the packet terminator
     * processing instruction into the single canonical line break, so `</x:xmpmeta>` and the
     * terminator keep sitting on their own lines. The padding itself carries no information
     * and must not distort the size measurement of the partitioning.
     */
    private fun stripPadding(packet: String): String {

        val endIndex = packet.indexOf(PACKET_END_MARKER)

        if (endIndex == -1)
            return packet

        var contentEnd = endIndex

        while (contentEnd > 0 && packet[contentEnd - 1].isWhitespace())
            contentEnd--

        return packet.substring(0, contentEnd) + "\n" + packet.substring(endIndex)
    }

    /**
     * Extracts the GUID of the `xmpNote:HasExtendedXMP` reference from the raw packet text.
     * Both serialization forms that writers emit are recognized: the shorthand attribute form
     * and the element form. Returns null if the packet does not reference extended data.
     */
    private fun findExtendedXmpGuid(packet: String): String? {

        attributeReferenceRegex.find(packet)?.let { return it.groupValues[1] }

        return elementReferenceRegex.find(packet)?.groupValues?.get(1)
    }

    /**
     * Splits the content between the `rdf:RDF` tags into blocks that each start with an
     * `rdf:Description` element.
     */
    private fun splitDescriptions(content: String): List<String> {

        val blocks = mutableListOf<String>()

        var blockStart = content.indexOf(DESCRIPTION_OPEN_TAG)

        while (blockStart != -1) {

            val nextBlockStart = content.indexOf(DESCRIPTION_OPEN_TAG, blockStart + 1)

            val blockEnd = if (nextBlockStart == -1) content.length else nextBlockStart

            blocks.add(content.substring(blockStart, blockEnd))

            blockStart = nextBlockStart
        }

        return blocks
    }

    /**
     * Builds the `rdf:Description` element that links the main packet to its extended data
     * through the given GUID, using the same serialization form as ExifTool.
     */
    private fun buildReferenceDescription(guid: String): String =
        """<rdf:Description rdf:about="" xmlns:xmpNote="${XMPConst.NS_XMP_NOTE}">""" +
            "<xmpNote:HasExtendedXMP>$guid</xmpNote:HasExtendedXMP>" +
            "</rdf:Description>"

    /**
     * Returns the index behind the `>` of the given tag's first occurrence.
     */
    private fun locateTagEnd(xml: String, tagName: String): Int {

        val tagStart = xml.indexOf(tagName)

        if (tagStart == -1)
            return -1

        val tagEnd = xml.indexOf('>', tagStart)

        return if (tagEnd == -1) -1 else tagEnd + 1
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {

        if (size < prefix.size)
            return false

        for (index in prefix.indices)
            if (this[index] != prefix[index])
                return false

        return true
    }

    @Suppress("MagicNumber")
    private fun readIntBE(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)

    @Suppress("MagicNumber")
    private fun writeIntBE(value: Int, target: ByteArray, offset: Int) {

        target[offset] = (value shr 24).toByte()
        target[offset + 1] = (value shr 16).toByte()
        target[offset + 2] = (value shr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    /**
     * One decoded slice of Adobe extended XMP data.
     */
    private class ExtendedXmpFragment(
        val guid: String,
        val totalLength: Int,
        val offset: Int,
        val data: ByteArray
    )
}
