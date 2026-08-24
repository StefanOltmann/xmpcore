package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Adobe's XMPCore deliberately excludes tab, LF and CR from control-char sanitization
 * so multi-line values survive round trips. Property values written through the API
 * must therefore keep their line structure.
 */
class XMPMetaWhitespacePreservationTest {

    /**
     * Builds a value containing tab, LF and CR without escape-heavy literals.
     */
    private fun buildMultiLineValue(): String =
        buildString {
            append("first line")
            append('\n')
            append("second line")
            append('\t')
            append("indented")
            append('\r')
            append('\n')
            append("third line")
        }

    @Test
    fun testLineBreaksInPropertyValuesArePreserved() {

        val xmp = XMPMetaFactory.create()

        val multiLine = buildMultiLineValue()

        xmp.setProperty(XMPConst.NS_DC, "rights", multiLine)

        assertEquals(multiLine, xmp.getPropertyString(XMPConst.NS_DC, "rights"))
    }

    @Test
    fun testLineBreaksSurviveSerializeAndParseRoundTrip() {

        val original = XMPMetaFactory.create()

        original.setDescription("Shot at dawn\nGolden hour")

        val serialized = XMPMetaFactory.serializeToString(original, null)

        val reparsed = XMPMetaFactory.parseFromString(serialized)

        assertEquals("Shot at dawn\nGolden hour", reparsed.getDescription())
    }
}
