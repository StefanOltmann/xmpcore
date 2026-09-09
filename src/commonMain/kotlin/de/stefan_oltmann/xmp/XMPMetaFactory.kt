/*
 * =================================================================================================
 * ADOBE SYSTEMS INCORPORATED
 * Copyright 2006 Adobe Systems Incorporated
 * All Rights Reserved
 *
 * NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
 * of the Adobe license agreement accompanying it.
 * =================================================================================================
 */
package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.ExtendedXmpCodec
import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.internal.XMPMetaParser
import de.stefan_oltmann.xmp.internal.XMPRDFWriter
import de.stefan_oltmann.xmp.options.ParseOptions
import de.stefan_oltmann.xmp.options.SerializeOptions

/**
 * Creates `XMPMeta`-instances and serializes them to a String. This is the entry point for
 * parsing XMP packets.
 */
public object XMPMetaFactory {

    @kotlin.jvm.JvmStatic
    public val schemaRegistry: XMPSchemaRegistry = XMPSchemaRegistry

    @kotlin.jvm.JvmStatic
    public val versionInfo: XMPVersionInfo = XMPVersionInfo

    /**
     * Creates an empty `XMPMeta`-object.
     */
    @kotlin.jvm.JvmStatic
    public fun create(): XMPMeta = XMPMeta()

    /**
     * Creates an `XMPMeta`-object from a string.
     *
     * If the input contains more than one complete XMP packet, for example because a
     * container concatenated several packets, the first complete packet is parsed and the
     * remaining data is ignored, exactly like ExifTool does it. The same holds for junk
     * before the first packet, like the NUL padding of corrupted files.
     *
     * @param packet A String containing an XMP-file.
     * @param options Options controlling the parsing.
     * @return Returns the `XMPMeta`-object created from the input.
     * @throws XMPException If the file is not well-formed XML or if the parsing fails.
     */
    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun parseFromString(
        packet: String,
        options: ParseOptions? = null
    ): XMPMeta {

        try {

            return XMPMetaParser.parse(packet, options)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Parsing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }

    /**
     * Parses a main XMP packet that references Adobe extended XMP data together with its
     * extended chunks. The chunks are validated for consistency (contiguous offsets, declared
     * total length, MD5 GUID) and merged into the main packet before parsing, so the returned
     * [XMPMeta] contains the properties of both. The consumed `xmpNote:HasExtendedXMP`
     * reference is not part of the merged packet, because it would point at chunks that no
     * longer exist when the merged packet is written back.
     *
     * A main packet without a reference is parsed as-is and the chunks are ignored.
     *
     * @param packet A String containing the main XMP packet.
     * @param extendedChunks The raw extended XMP chunks in Adobe's chunk format, as delivered
     * by [partitionPacket], in any order.
     * @param options Options controlling the parsing.
     * @return Returns the [XMPMeta] created from the merged packet.
     * @throws XMPException If the reference cannot be satisfied, the chunks are corrupt, or
     * the parsing fails.
     */
    @kotlin.jvm.JvmStatic
    @Throws(XMPException::class)
    public fun parseFromString(
        packet: String,
        extendedChunks: List<ByteArray>,
        options: ParseOptions? = null
    ): XMPMeta {

        try {

            return XMPMetaParser.parse(ExtendedXmpCodec.merge(packet, extendedChunks), options)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /* Ensure that only XMPException is thrown from this method. */

            throw XMPException("Parsing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }

    /**
     * Splits a serialized XMP packet that exceeds a size limit into a main packet and Adobe
     * extended XMP chunks, exactly like ExifTool and the Adobe SDK do it: the main packet
     * keeps as many whole `rdf:Description` blocks as fit and references the remaining data
     * through `xmpNote:HasExtendedXMP`; the moved blocks are serialized into chunks that
     * carry the MD5 GUID of the extended data, its total length and one slice of the data
     * each. A stale reference from a previous write is always removed, and a missing line
     * break before the packet terminator is added, so the terminator always starts its own
     * line. Packets that fit are otherwise returned with their original bytes; only an
     * oversized packet has its in-place editing padding collapsed into that line break,
     * because the padding would distort the size measurement.
     *
     * @param packet A String containing the XMP packet to split.
     * @param maxMainPacketBytes The maximum UTF-8 byte size of the main packet. For a JPEG
     * APP1 segment this is 65533 minus the length of the container's XMP identifier.
     * @param maxExtendedChunkBytes The maximum number of extended data bytes per chunk. The
     * chunk header (75 bytes) on top of it must also fit into the container's storage unit.
     * @return Returns the [XmpPacketPartition] with the main packet and the extended chunks.
     * @throws XMPException If the packet exceeds the limit but uses RDF structures the
     * splitter cannot partition.
     */
    @kotlin.jvm.JvmStatic
    @Throws(XMPException::class)
    public fun partitionPacket(
        packet: String,
        maxMainPacketBytes: Int,
        maxExtendedChunkBytes: Int
    ): XmpPacketPartition =
        ExtendedXmpCodec.partition(packet, maxMainPacketBytes, maxExtendedChunkBytes)

    /**
     * Merges a main XMP packet that references Adobe extended XMP data with its chunks into
     * one packet. The chunks are validated for consistency (contiguous offsets, declared
     * total length, MD5 GUID) before their `rdf:Description` blocks are inserted, and the
     * consumed `xmpNote:HasExtendedXMP` reference is removed from the result. This is the
     * inverse of [partitionPacket] and useful to extract the complete XMP of a container
     * into a sidecar file, for example.
     *
     * A main packet without a reference is returned unchanged and the chunks are ignored.
     *
     * @param mainPacket A String containing the main XMP packet.
     * @param extendedChunks The raw extended XMP chunks in Adobe's chunk format, as delivered
     * by [partitionPacket], in any order.
     * @return Returns the merged packet text, ready for [parseFromString].
     * @throws XMPException If the reference cannot be satisfied or the chunks are corrupt.
     */
    @kotlin.jvm.JvmStatic
    @Throws(XMPException::class)
    public fun assemblePacket(
        mainPacket: String,
        extendedChunks: List<ByteArray>
    ): String =
        ExtendedXmpCodec.merge(mainPacket, extendedChunks)

    /**
     * Serializes an `XMPMeta`-object as RDF into a string.
     * Note: Encoding is ignored when serializing to a string.
     *
     * @param xmp A metadata object.
     * @param options Options to control the serialization (see [SerializeOptions]).
     * @return Returns a String containing the serialized RDF.
     * @throws XMPException On serialization errors.
     */
    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun serializeToString(
        xmp: XMPMeta,
        options: SerializeOptions? = null
    ): String {

        try {

            val actualOptions = options ?: SerializeOptions()

            /* Sort the internal data model on demand */
            if (actualOptions.getSort())
                xmp.sort()

            return XMPRDFWriter(actualOptions).serialize(xmp)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Serializing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }
}
