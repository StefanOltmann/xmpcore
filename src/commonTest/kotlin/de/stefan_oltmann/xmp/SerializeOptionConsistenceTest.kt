package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the option consistency rules of the serializer ported from the Adobe original:
 * a read-only packet requires the wrapper and never carries padding, and the packet
 * trailer is indented by the base indent.
 */
class SerializeOptionConsistenceTest {

    /**
     * A read-only packet without wrapper is an inconsistent option combination.
     */
    @Test
    fun testReadOnlyPacketWithOmittedWrapperThrows() {

        val options = SerializeOptions()
            .setReadOnlyPacket(true)
            .setOmitPacketWrapper(true)

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.serializeToString(XMPMetaFactory.create(), options)
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * A read-only packet carries no padding, while a writeable packet with the same
     * padding emits it before the trailer.
     */
    @Test
    fun testReadOnlyPacketEmitsNoPadding() {

        /* A padding of 101 emits exactly one 100-space chunk plus a newline. */
        val paddingRun = " ".repeat(100)

        val writeable = XMPMetaFactory.serializeToString(
            XMPMetaFactory.create(),
            SerializeOptions().setPadding(101)
        )

        assertTrue(writeable.contains(paddingRun))

        val readOnly = XMPMetaFactory.serializeToString(
            XMPMetaFactory.create(),
            SerializeOptions().setReadOnlyPacket(true).setPadding(101)
        )

        assertTrue(!readOnly.contains(paddingRun))
    }

    /**
     * The packet trailer is indented by the base indent.
     */
    @Test
    fun testBaseIndentIndentsPacketTrailer() {

        val serialized = XMPMetaFactory.serializeToString(
            XMPMetaFactory.create(),
            SerializeOptions().setBaseIndent(2)
        )

        assertTrue(serialized.contains("\n    <?xpacket end"))
    }
}
