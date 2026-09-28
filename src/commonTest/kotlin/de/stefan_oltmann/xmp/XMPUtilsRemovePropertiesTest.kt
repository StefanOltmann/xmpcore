package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [XMPUtils.removeProperties] against the behavior of the Adobe XMPCore original,
 * including the internal-property protection.
 */
class XMPUtilsRemovePropertiesTest {

    /**
     * A single named property is removed, its siblings survive.
     */
    @Test
    fun testRemoveSingleProperty() {

        val xmpMeta = parse(
            """
                <dc:title>Haltestelle</dc:title>
                <dc:subject>
                <rdf:Bag>
                  <rdf:li>fox</rdf:li>
                </rdf:Bag>
                </dc:subject>
            """.trimIndent()
        )

        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_DC, "dc:title", false, false)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "subject"))
    }

    /**
     * A property the XMP specification marks as internal is only removed when
     * doAllProperties is set.
     */
    @Test
    fun testRemoveSingleInternalPropertyIsProtected() {

        val xmpMeta = parse(
            """
                <xmp:ModifyDate>2020-05-01T08:15:30</xmp:ModifyDate>
            """.trimIndent()
        )

        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_XMP, "xmp:ModifyDate", false, false)

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))

        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_XMP, "xmp:ModifyDate", true, false)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))
    }

    /**
     * Removing a whole schema keeps internal properties and keeps the schema alive, and a
     * schema that becomes empty is removed entirely.
     */
    @Test
    fun testRemoveSchemaKeepsInternalProperties() {

        val xmpMeta = parse(
            """
                <dc:format>image/jpeg</dc:format>
                <dc:title>Haltestelle</dc:title>
                <xmp:ModifyDate>2020-05-01T08:15:30</xmp:ModifyDate>
            """.trimIndent()
        )

        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_DC, null, false, false)

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "format"))

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))

        /* With doAllProperties the last internal property goes and the empty schema dies. */
        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_XMP, null, true, false)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))
    }

    /**
     * Without schema and property names all non-internal properties of all schemas are
     * removed.
     */
    @Test
    fun testRemoveAllSchemas() {

        val xmpMeta = parse(
            """
                <dc:title>Haltestelle</dc:title>
                <dc:format>image/jpeg</dc:format>
                <xmp:ModifyDate>2020-05-01T08:15:30</xmp:ModifyDate>
            """.trimIndent()
        )

        XMPUtils.removeProperties(xmpMeta, null, null, false, false)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "format"))

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))
    }

    /**
     * A property name without a schema namespace is rejected.
     */
    @Test
    fun testRemoveSinglePropertyRequiresSchemaNamespace() {

        val xmpMeta = parse("")

        val ex = assertFailsWith<XMPException> {
            XMPUtils.removeProperties(xmpMeta, null, "dc:title", false, false)
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * includeAliases removes the properties the schema's aliases point to, so an aliased
     * value does not survive under its base name.
     */
    @Test
    fun testRemoveSchemaIncludesAliasTargets() {

        val xmpMeta = parse(
            """
                <dc:creator>
                <rdf:Seq>
                  <rdf:li>Some Photographer</rdf:li>
                </rdf:Seq>
                </dc:creator>
                <tiff:Artist>Some Photographer</tiff:Artist>
                <tiff:Model>Canon EOS</tiff:Model>
            """.trimIndent()
        )

        XMPUtils.removeProperties(xmpMeta, XMPConst.NS_TIFF, null, false, true)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "creator"))

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_TIFF, "Artist"))

        assertTrue(xmpMeta.doesPropertyExist(XMPConst.NS_TIFF, "Model"))
    }

    private fun parse(properties: String): XMPMeta {

        val rdf = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:tiff="http://ns.adobe.com/tiff/1.0/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
            $properties
                </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        return XMPMetaFactory.parseFromString(rdf)
    }
}
