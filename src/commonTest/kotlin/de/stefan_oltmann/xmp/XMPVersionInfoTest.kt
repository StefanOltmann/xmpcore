package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the released version, so an accidental version bump in [XMPVersionInfo] becomes
 * visible in review and the serialized x:xmptk attribute is a conscious decision.
 */
class XMPVersionInfoTest {

    /**
     * The version message reports the released version of this port.
     */
    @Test
    fun testVersionMessage() {

        assertEquals("XMP Core for KMP 2.0.0", XMPVersionInfo.VERSION_MESSAGE)
    }
}
