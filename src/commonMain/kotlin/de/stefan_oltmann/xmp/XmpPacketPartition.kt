package de.stefan_oltmann.xmp

/**
 * The result of splitting an oversized XMP packet with [XMPMetaFactory.partitionPacket].
 *
 * The main packet keeps as many whole `rdf:Description` blocks as fit into the given size
 * limit and references the moved data through `xmpNote:HasExtendedXMP`, exactly like ExifTool
 * and the Adobe SDK do it. The moved data is serialized into chunks in Adobe's extended XMP
 * chunk format, which containers embed as additional storage units (one chunk per JPEG APP1
 * segment, for example).
 */
public class XmpPacketPartition(

    /**
     * The main packet, ready to be embedded into the container's primary XMP storage.
     */
    public val mainPacket: String,

    /**
     * The extended XMP chunks in Adobe's chunk format (signature, GUID, total length, offset
     * and one slice of the extended data each), in order. Empty when the packet fits into the
     * size limit without splitting.
     */
    public val extendedChunks: List<ByteArray>
)
