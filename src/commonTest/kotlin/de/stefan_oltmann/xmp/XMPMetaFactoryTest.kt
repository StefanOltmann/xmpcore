package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.ParseOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the parse and serialize entry points of [XMPMetaFactory].
 */
class XMPMetaFactoryTest {

    /**
     * [XMPMetaFactory.create] returns an empty metadata object.
     */
    @Test
    fun testCreateReturnsEmptyMeta() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals("", xmpMeta.getObjectName())
        assertNotNull(xmpMeta.iterator())
        assertTrue(!xmpMeta.iterator().hasNext())
    }

    /**
     * Parsing without options behaves like the default parse options.
     */
    @Test
    fun testParseWithNullOptions() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                xmp:Rating="3"/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp, null)

        assertEquals(3, xmpMeta.getRating())
    }

    /**
     * A bare rdf:RDF document is accepted without the REQUIRE_XMP_META option.
     */
    @Test
    fun testParseBareRdfWithoutRequireXmpMeta() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""/>
            </rdf:RDF>
        """.trimIndent()

        assertNotNull(XMPMetaFactory.parseFromString(testXmp))
    }

    /**
     * A bare rdf:RDF document is rejected with the REQUIRE_XMP_META option.
     */
    @Test
    fun testParseBareRdfWithRequireXmpMetaThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""/>
            </rdf:RDF>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp, ParseOptions().setRequireXMPMeta(true))
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * A blank input is rejected.
     */
    @Test
    fun testParseBlankInputThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString("   ")
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * An XML document without RDF content is rejected.
     */
    @Test
    fun testParseDocumentWithoutRdfThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString("<foo/>")
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * Invalid XML is rejected as a stream error.
     */
    @Test
    fun testParseInvalidXmlThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString("this is not xml")
        }

        assertEquals(XMPErrorConst.BADSTREAM, ex.errorCode)
    }

    /**
     * The legacy x:xapmeta element is accepted as envelope.
     */
    @Test
    fun testParseWithXapmetaEnvelope() {

        /* language=XML */
        val testXmp = """
            <x:xapmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 6.0.0">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:title>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Titel</rdf:li>
                    </rdf:Alt>
                  </dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xapmeta>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An XMP packet wrapped in a foreign document, e.g. SVG, is found.
     */
    @Test
    fun testParseXmpInsideForeignDocument() {

        /* language=XML */
        val testXmp = """
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:x="adobe:ns:meta/">
              <metadata>
                <x:xmpmeta>
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:dc="http://purl.org/dc/elements/1.1/">
                      <dc:title>
                        <rdf:Alt>
                          <rdf:li xml:lang="x-default">Titel</rdf:li>
                        </rdf:Alt>
                      </dc:title>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
              </metadata>
            </svg>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * Serializing without options writes the default packet form.
     */
    @Test
    fun testSerializeWithNullOptions() {

        val xmpMeta = XMPMetaFactory.create()

        val serialized = XMPMetaFactory.serializeToString(xmpMeta, null)

        assertTrue(serialized.contains("<?xpacket begin="))
        assertTrue(serialized.contains("<x:xmpmeta"))
        assertTrue(serialized.contains("<?xpacket end=\"w\"?>"))
    }

    /**
     * The schema registry and version info singletons are exposed.
     */
    @Test
    fun testFactoryExposesSingletons() {

        assertSame(XMPSchemaRegistry, XMPMetaFactory.schemaRegistry)
        assertSame(XMPVersionInfo, XMPMetaFactory.versionInfo)
    }
}
