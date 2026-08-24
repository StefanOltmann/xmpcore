package de.stefan_oltmann.xmp.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun testReplaceControlCharsWithSpace() {

        /* Normal chars - nothing should be changed. */
        assertEquals(
            "Example GmbH & Co. KG",
            Utils.replaceControlCharsWithSpace("Example GmbH & Co. KG")
        )

        /* Control characters in between are replaced by a single space */
        assertEquals(
            "Example GmbH & Co. KG",
            Utils.replaceControlCharsWithSpace("Example\u0000GmbH\u001B&\u0007Co.\u0004KG")
        )

        /* Individual control characters */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0000")) /* NUL */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0001")) /* SOH */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0002")) /* STX */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0003")) /* ETX */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0004")) /* EOT */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0005")) /* ENQ */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0006")) /* ACK */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0007")) /* BEL */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0008")) /* BS */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000B")) /* VT */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000C")) /* FF */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000E")) /* SO */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u000F")) /* SI */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0010")) /* DLE */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0011")) /* DC1 */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0012")) /* DC2 */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0013")) /* DC3 */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0014")) /* DC4 */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0015")) /* NAK */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0016")) /* SYN */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0017")) /* ETB */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0018")) /* CAN */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u0019")) /* EM */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001A")) /* SUB */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001B")) /* ESC */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001C")) /* FS */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001D")) /* GS */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001E")) /* RS */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u001F")) /* US */
        assertEquals(" ", Utils.replaceControlCharsWithSpace("\u007F")) /* DEL */

        /*
         * Tab, LF and CR survive like in the Adobe original,
         * so multi-line values survive round trips.
         */
        assertEquals("\u0009", Utils.replaceControlCharsWithSpace("\u0009")) /* HT */
        assertEquals("\u000A", Utils.replaceControlCharsWithSpace("\u000A")) /* LF */
        assertEquals("\u000D", Utils.replaceControlCharsWithSpace("\u000D")) /* CR */
    }

    @Test
    fun testLineStructureSurvives() {

        val multiLine = buildString {
            append("first line")
            append('\n')
            append("second line")
            append('\t')
            append("indented")
            append('\r')
            append('\n')
            append("third line")
        }

        assertEquals(multiLine, Utils.replaceControlCharsWithSpace(multiLine))

        /* Real control characters are still cleaned up in between. */
        assertEquals(
            buildString {
                append("first")
                append(' ')
                append(" line second")
                append(' ')
                append(" line")
            },
            Utils.replaceControlCharsWithSpace(
                buildString {
                    append("first")
                    append('\u0000')
                    append(" line second")
                    append('\u0007')
                    append(" line")
                }
            )
        )
    }
}
