package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The corrupted-document salvage must classify unparseable input as
 * [XMPException] with the documented BADXML code instead of crashing,
 * and numeric character references for supplementary-plane characters
 * (above the BMP) must survive the control-character repair.
 */
class HostileInputRecoveryTest {

    /**
     * A stray end tag before the real RDF element must not crash the
     * salvage slice; the input is unparseable and must surface as the
     * documented BADXML error, not an index exception re-wrapped as
     * UNKNOWN.
     */
    @Test
    fun testEndTagBeforeStartTagFailsAsBadXml() {

        val input = "</rdf:RDF>a<rdf:RDF/>"

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(input)
        }

        assertEquals(XMPErrorConst.BADXML, exception.errorCode)
    }

    /**
     * The junk-around-RDF recovery must still salvage the RDF island
     * when the junk ends with a fragment that looks like an end tag.
     */
    @Test
    fun testSalvagesRdfIslandBehindEndTagFragment() {

        val input = "</rdf:RDF>a" + """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:source>x</dc:source>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val meta = XMPMetaFactory.parseFromString(input)

        assertEquals("x", meta.getPropertyString(XMPConst.NS_DC, "source"))
    }

    /**
     * Numeric character references for supplementary-plane characters
     * are valid XML and must survive the control-character repair -
     * their Int code points wrap around when truncated to a Char, which
     * used to turn them into spaces.
     */
    @Test
    fun testSupplementaryPlaneReferencesSurvive() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:source>&#x10000;&#x7F;</dc:source>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val meta = XMPMetaFactory.parseFromString(testXmp)

        /*
         * U+10000 (Linear B syllable) survives; U+7F is a control
         * character and is repaired to a space.
         */
        assertEquals("\uD800\uDC00 ", meta.getPropertyString(XMPConst.NS_DC, "source"))
    }
}
