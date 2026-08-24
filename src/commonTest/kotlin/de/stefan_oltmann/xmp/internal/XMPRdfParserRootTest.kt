package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
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

    /**
     * A root element that is no rdf:RDF element is rejected.
     */
    @Test
    fun testParseNonRdfElementRootThrows() {

        val document = DomParser.parseDocumentFromString("<foo/>")

        val ex = assertFailsWith<XMPException> {
            XMPRDFParser.parse(requireNotNull(document.getDocumentElement()), ParseOptions())
        }

        assertEquals(XMPErrorConst.BADRDF, ex.errorCode)
    }

    /**
     * An rdf:RDF element without attributes is rejected.
     */
    @Test
    fun testParseRdfElementWithoutAttributesThrows() {

        val document = DomParser.parseDocumentFromString("<foo/>")

        val rdfElement = document.createElementNS(XMPConst.NS_RDF, "rdf:RDF")

        val ex = assertFailsWith<XMPException> {
            XMPRDFParser.parse(rdfElement, ParseOptions())
        }

        assertEquals(XMPErrorConst.BADRDF, ex.errorCode)
    }
}
