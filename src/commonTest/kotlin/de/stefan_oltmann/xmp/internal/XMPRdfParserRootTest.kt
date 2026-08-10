package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.options.ParseOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the direct entry point of [XMPRDFParser].
 */
class XMPRdfParserRootTest {

    /**
     * A non-element root node is rejected.
     */
    @Test
    fun testParseNonElementRootThrows() {

        val document = DomParser.parseDocumentFromString("<foo/>")

        val ex = assertFailsWith<XMPException> {
            XMPRDFParser.parse(document, ParseOptions())
        }

        assertEquals(XMPErrorConst.BADRDF, ex.errorCode)
    }
}
