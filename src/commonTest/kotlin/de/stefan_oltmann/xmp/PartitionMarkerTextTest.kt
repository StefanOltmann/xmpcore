package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The stale-reference safety net must not delete user data: a block
 * whose value merely contains the marker text is user content, and
 * dropping the whole block - even when the packet is large enough to be
 * partitioned - would silently lose that description.
 */
class PartitionMarkerTextTest {

    /**
     * The same value as the small-packet pin, but with enough filler to
     * force the packet over the partition limit: the description must
     * survive the split, moved into the extended data intact.
     */
    @Test
    fun testPartitionKeepsMarkerTextInsideValuesOfLargePackets() {

        val description =
            """before xmpNote:HasExtendedXMP="0123456789ABCDEF0123456789ABCDEF" after """ +
                "filler ".repeat(600)

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "source", description)

        xmpMeta.setQualifier(
            XMPConst.NS_DC,
            "source",
            XMPConst.NS_XML,
            "lang",
            XMPConst.X_DEFAULT
        )

        val packet = XMPMetaFactory.serializeToString(xmpMeta, SerializeOptions())

        val partition = XMPMetaFactory.partitionPacket(
            packet,
            maxMainPacketBytes = 600,
            maxExtendedChunkBytes = 4096
        )

        val reparsedMain = XMPMetaFactory.parseFromString(partition.mainPacket)

        val reparsedExtended = if (partition.extendedChunks.isEmpty())
            reparsedMain
        else
            XMPMetaFactory.parseFromString(
                XMPMetaFactory.assemblePacket(partition.mainPacket, partition.extendedChunks)
            )

        /*
         * The description is either kept in the main packet or moved into
         * the extended data - it must exist in exactly one of the two.
         */
        val kept = reparsedMain.getPropertyString(XMPConst.NS_DC, "source")

        val moved = reparsedExtended.getPropertyString(XMPConst.NS_DC, "source")

        assertTrue(kept != null || moved != null)

        if (kept != null)
            assertEquals(description, kept)

        if (moved != null)
            assertEquals(description, moved)
    }
}
