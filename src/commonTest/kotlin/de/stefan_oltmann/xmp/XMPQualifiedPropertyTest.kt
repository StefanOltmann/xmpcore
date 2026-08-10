package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the qualified property forms parsed from rdf:value and the qualifier
 * handling of the RDF parser.
 */
class XMPQualifiedPropertyTest {

    /**
     * A qualified property in the rdf:value element form is fixed up.
     */
    @Test
    fun testQualifiedPropertyViaRdfValueElement() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:prop>
                  <rdf:Description rdf:value="Der Titel">
                    <dc:sub>Sub</dc:sub>
                  </rdf:Description>
                </xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("Der Titel", xmpMeta.getPropertyString(XMPConst.NS_XMP, "prop"))
        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "prop", XMPConst.NS_DC, "sub"))
        assertEquals(
            expected = "Sub",
            actual = xmpMeta.getQualifier(XMPConst.NS_XMP, "prop", XMPConst.NS_DC, "sub")!!.getValue()
        )
    }

    /**
     * A qualified property in the rdf:value attribute form is fixed up.
     */
    @Test
    fun testQualifiedPropertyViaRdfValueAttribute() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:Rating rdf:value="5" xmp:note="n1"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(5, xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "Rating"))
        assertEquals(
            expected = "n1",
            actual = xmpMeta.getQualifier(XMPConst.NS_XMP, "Rating", XMPConst.NS_XMP, "note")!!.getValue()
        )
    }

    /**
     * A qualified property with a language qualifier keeps it after the fixup.
     */
    @Test
    fun testQualifiedPropertyKeepsLanguageQualifier() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop>
                  <rdf:Description rdf:value="Titel" xml:lang="de"/>
                </xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("Titel", xmpMeta.getPropertyString(XMPConst.NS_XMP, "prop"))
        assertEquals(
            expected = "de",
            actual = xmpMeta.getQualifier(XMPConst.NS_XMP, "prop", XMPConst.NS_XML, "lang")!!.getValue()
        )
    }

    /**
     * The language qualifier of an rdf:value element is moved to the parent.
     */
    @Test
    fun testRdfValueLangQualifierMovedToParent() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop>
                  <rdf:Description>
                    <rdf:value xml:lang="de">Wert</rdf:value>
                  </rdf:Description>
                </xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("Wert", xmpMeta.getPropertyString(XMPConst.NS_XMP, "prop"))
        assertEquals(
            expected = "de",
            actual = xmpMeta.getQualifier(XMPConst.NS_XMP, "prop", XMPConst.NS_XML, "lang")!!.getValue()
        )
    }

    /**
     * An rdf:value element with property attributes becomes a struct whose
     * fields keep the attributes.
     */
    @Test
    fun testRdfValueWithPropertyAttributes() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:prop>
                  <rdf:Description>
                    <rdf:value dc:q="v1" xmp:r="v2"/>
                  </rdf:Description>
                </xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "v1",
            actual = xmpMeta.getStructField(XMPConst.NS_XMP, "prop", XMPConst.NS_DC, "q")!!.getValue()
        )
        assertEquals(
            expected = "v2",
            actual = xmpMeta.getStructField(XMPConst.NS_XMP, "prop", XMPConst.NS_XMP, "r")!!.getValue()
        )
    }

    /**
     * An rdf:resource property with a qualifier attribute keeps both.
     */
    @Test
    fun testResourcePropertyWithQualifierAttribute() {

        val qualifierNamespace = "http://example.org/xmpcore-resource-qual/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "resq")

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:resq="$qualifierNamespace">
                <xmp:p resq:q="v" rdf:resource="u"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val property = xmpMeta.getProperty(XMPConst.NS_XMP, "p")!!

        assertEquals("u", property.getValue())
        assertTrue(property.getOptions().isURI())
        assertEquals(
            expected = "v",
            actual = xmpMeta.getQualifier(XMPConst.NS_XMP, "p", qualifierNamespace, "q")!!.getValue()
        )
    }

    /**
     * An xml:lang attribute on a resource property element becomes a qualifier.
     */
    @Test
    fun testLangQualifierOnResourceProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title xml:lang="de">
                  <rdf:Alt>
                    <rdf:li xml:lang="de">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_DC, "title", XMPConst.NS_XML, "lang"))
    }

    /**
     * A resource property element with a parseType Resource keeps an xml:lang
     * qualifier.
     */
    @Test
    fun testParseTypeResourceWithLangQualifier() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:s rdf:parseType="Resource" xml:lang="de">
                  <dc:f>v</dc:f>
                </xmp:s>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "s", XMPConst.NS_XML, "lang"))
    }

    /**
     * An xml:lang attribute on an empty property element with struct fields is
     * kept as qualifier.
     */
    @Test
    fun testLangQualifierOnStructFromAttributes() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:s dc:f="v" xml:lang="de"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "v",
            actual = xmpMeta.getStructField(XMPConst.NS_XMP, "s", XMPConst.NS_DC, "f")!!.getValue()
        )
        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "s", XMPConst.NS_XML, "lang"))
    }

    /**
     * An rdf:nodeID attribute on an empty property element is ignored.
     */
    @Test
    fun testNodeIdOnEmptyProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop rdf:nodeID="n1"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getPropertyString(XMPConst.NS_XMP, "prop"))
    }
}
