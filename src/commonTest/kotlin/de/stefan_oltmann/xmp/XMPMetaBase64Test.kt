package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the base64 property access against the behavior of the Adobe XMPCore original: a
 * missing value throws, and '=' characters are ignored everywhere in the data.
 */
class XMPMetaBase64Test {

    /**
     * Reading a base64 property without a value throws instead of returning an empty
     * array, like the Adobe original.
     */
    @Test
    fun testGetPropertyBase64ThrowsOnMissingValue() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "Base64Test", null)

        val ex = assertFailsWith<XMPException> {
            xmpMeta.getPropertyBase64(XMPConst.NS_XMP, "Base64Test")
        }

        assertEquals(XMPErrorConst.BADVALUE, ex.errorCode)
    }

    /**
     * Padding characters anywhere in the value are ignored, like Adobe's decoder does.
     */
    @Test
    fun testBase64IgnoresPaddingCharactersEverywhere() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "Base64Test", "AB=CD==")

        val bytes = checkNotNull(xmpMeta.getPropertyBase64(XMPConst.NS_XMP, "Base64Test"))

        /* "ABCD" decodes to 0x00 0x10 0x83. */
        assertEquals(3, bytes.size)

        assertEquals(0.toByte(), bytes[0])

        assertEquals(16.toByte(), bytes[1])

        assertEquals((-125).toByte(), bytes[2])
    }
}
