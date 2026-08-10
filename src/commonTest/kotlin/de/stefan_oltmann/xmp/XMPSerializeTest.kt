package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the RDF serialization [XMPMetaFactory.serializeToString] and the
 * [SerializeOptions] behavior.
 */
class XMPSerializeTest {

    /**
     * The packet wrapper can be omitted.
     */
    @Test
    fun testSerializeOmitPacketWrapper() {

        val xmpMeta = XMPMetaFactory.create()

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setOmitPacketWrapper(true)
        )

        assertFalse(serialized.contains("xpacket"))
        assertTrue(serialized.contains("<x:xmpmeta"))
    }

    /**
     * The xmpmeta element can be omitted.
     */
    @Test
    fun testSerializeOmitXmpMetaElement() {

        val xmpMeta = XMPMetaFactory.create()

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setOmitXmpMetaElement(true)
        )

        assertFalse(serialized.contains("x:xmpmeta"))
        assertTrue(serialized.contains("<rdf:RDF"))
        assertTrue(serialized.contains("<?xpacket"))
    }

    /**
     * A read-only packet is marked with the r trailer.
     */
    @Test
    fun testSerializeReadOnlyPacket() {

        val xmpMeta = XMPMetaFactory.create()

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setReadOnlyPacket(true)
        )

        assertTrue(serialized.contains("<?xpacket end=\"r\"?>"))
    }

    /**
     * XML special characters in attribute values are escaped.
     */
    @Test
    fun testSerializeEscapesAttributeValues() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "a\"b&c<d>e")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("xmp:prop=\"a&quot;b&amp;c&lt;d&gt;e\""))
    }

    /**
     * Whitespace control chars parsed from XML are re-escaped on serialize.
     */
    @Test
    fun testSerializeEscapesWhitespaceInText() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop>a&#x9;b&#xA;c</xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("a&#x9;b&#xA;c"))
    }

    /**
     * A property with an empty value is serialized as empty attribute.
     */
    @Test
    fun testSerializeEmptyValueProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("xmp:prop=\"\""))
    }

    /**
     * A URI property is serialized with the rdf:resource attribute.
     */
    @Test
    fun testSerializeUriProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:Webpage rdf:resource="http://example.org/"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:resource=\"http://example.org/\""))
    }

    /**
     * An empty array is serialized as an empty container.
     */
    @Test
    fun testSerializeEmptyArray() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null, PropertyOptions().setArray(true))

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("<dc:subject>"))
        assertTrue(serialized.contains("<rdf:Bag/>"))
    }

    /**
     * An empty struct is serialized with rdf:parseType="Resource" in compact form.
     */
    @Test
    fun testSerializeEmptyStructCompact() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "empty", null, PropertyOptions().setStruct(true))

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:parseType=\"Resource\"/>"))
    }

    /**
     * An empty struct is serialized with an inner rdf:Description in canonical form.
     */
    @Test
    fun testSerializeEmptyStructCanonical() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "empty", null, PropertyOptions().setStruct(true))

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setUseCanonicalFormat(true)
        )

        assertTrue(serialized.contains("<xmp:empty>"))
        assertTrue(serialized.contains("<rdf:Description/>"))
    }

    /**
     * A struct whose fields can all be attributes is serialized as an empty
     * property element.
     */
    @Test
    fun testSerializeStructWithAttributeFields() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field2", "b")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("xmp:struct"))
        assertTrue(serialized.contains("dc:field1=\"a\""))
        assertTrue(serialized.contains("dc:field2=\"b\""))
        assertFalse(serialized.contains("rdf:parseType=\"Resource\""))
    }

    /**
     * A struct with an element field is serialized with rdf:parseType="Resource".
     */
    @Test
    fun testSerializeStructWithElementFields() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-qual/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serQual")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct/dc:field1", qualifierNamespace, "q", "v")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:parseType=\"Resource\""))
    }

    /**
     * A struct with mixed attribute and element fields uses an inner
     * rdf:Description element.
     */
    @Test
    fun testSerializeStructWithMixedFields() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-mixed/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serMixed")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "attrField", "a")
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "elemField", "b")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct/dc:elemField", qualifierNamespace, "q", "v")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("<rdf:Description"))
        assertTrue(serialized.contains("dc:attrField=\"a\""))
    }

    /**
     * A property with a general qualifier is serialized with an rdf:value.
     */
    @Test
    fun testSerializeGeneralQualifier() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-general/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serGeneral")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "q", "qv")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:parseType=\"Resource\""))
        assertTrue(serialized.contains("<rdf:value>value</rdf:value>"))
        assertTrue(serialized.contains("<serGeneral:q>qv</serGeneral:q>"))
    }

    /**
     * The xml:lang qualifier is written as attribute of the array item.
     */
    @Test
    fun testSerializeLangQualifierAsAttribute() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("<rdf:li xml:lang=\"x-default\">Titel</rdf:li>"))
    }

    /**
     * Sorting orders the properties alphabetically.
     */
    @Test
    fun testSerializeSortOrdersProperties() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "zebra", "1")
        xmpMeta.setProperty(XMPConst.NS_DC, "apple", "1")

        val unsorted = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(unsorted.indexOf("dc:zebra") < unsorted.indexOf("dc:apple"))

        val sorted = XMPMetaFactory.serializeToString(xmpMeta, SerializeOptions().setSort(true))

        assertTrue(sorted.indexOf("dc:apple") < sorted.indexOf("dc:zebra"))
    }

    /**
     * The object name is written into the rdf:about attribute.
     */
    @Test
    fun testSerializeObjectName() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setObjectName("my name")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:about=\"my name\""))
    }

    /**
     * A qualified property parsed from the rdf:value form survives a round trip.
     */
    @Test
    fun testSerializeQualifiedPropertyRoundTrip() {

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

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:value"))
        assertTrue(serialized.contains("<dc:sub>Sub</dc:sub>"))
    }

    /**
     * A typed node keeps its rdf:type qualifier over a round trip.
     */
    @Test
    fun testSerializeTypedNodeRoundTrip() {

        val typeNamespace = "http://example.org/xmpcore-serialize-typed/"

        XMPSchemaRegistry.registerNamespace(typeNamespace, "serTyped")

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:serTyped="$typeNamespace">
                <xmp:thing>
                  <serTyped:MyType serTyped:field="v"/>
                </xmp:thing>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("<rdf:type>"))

        val reparsed = XMPMetaFactory.parseFromString(serialized)

        assertTrue(reparsed.doesQualifierExist(XMPConst.NS_XMP, "thing", XMPConst.NS_RDF, "type"))
    }

    /**
     * A struct in canonical format uses an inner rdf:Description element.
     */
    @Test
    fun testSerializeCanonicalStruct() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setUseCanonicalFormat(true)
        )

        assertTrue(serialized.contains("<rdf:Description>"))
    }

    /**
     * Mixing an rdf:resource qualifier with element fields is rejected.
     */
    @Test
    fun testSerializeResourceQualifierWithElementFieldsThrows() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-res/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serRes")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "struct", null, PropertyOptions().setStruct(true))
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct/dc:field1", qualifierNamespace, "q", "v")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct", XMPConst.NS_RDF, "resource", "urn:x")

        assertSerializeFailsWithBadRdf(xmpMeta, SerializeOptions())
    }

    /**
     * Mixing an rdf:resource qualifier with general qualifiers is rejected in
     * the canonical form.
     */
    @Test
    fun testSerializeResourceQualifierWithGeneralQualifierThrows() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-res2/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serRes2")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "q", "v")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", XMPConst.NS_RDF, "resource", "urn:x")

        assertSerializeFailsWithBadRdf(xmpMeta, SerializeOptions().setUseCanonicalFormat(true))
    }

    /**
     * A property with a general qualifier uses the inner rdf:Description form
     * in canonical serialization.
     */
    @Test
    fun testSerializeCanonicalQualifiedProperty() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-canqual/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serCanQual")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "q", "qv")

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setUseCanonicalFormat(true)
        )

        assertTrue(serialized.contains("<rdf:Description>"))
        assertTrue(serialized.contains("<rdf:value>value</rdf:value>"))
        assertTrue(serialized.contains("<serCanQual:q>qv</serCanQual:q>"))
    }

    /**
     * An empty struct with a general qualifier is serialized with the
     * parseType Resource form.
     */
    @Test
    fun testSerializeQualifiedEmptyStruct() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-emptystruct/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serEmptyStruct")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "s", null, PropertyOptions().setStruct(true))
        xmpMeta.setQualifier(XMPConst.NS_XMP, "s", qualifierNamespace, "q", "v")

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("rdf:parseType=\"Resource\"/>"))
    }

    /**
     * Mixing an rdf:resource qualifier with a complex field is rejected in the
     * canonical form.
     */
    @Test
    fun testSerializeCanonicalResourceWithComplexFieldThrows() {

        val qualifierNamespace = "http://example.org/xmpcore-serialize-canres/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "serCanRes")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "struct", null, PropertyOptions().setStruct(true))
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct/dc:field1", qualifierNamespace, "q", "v")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct", XMPConst.NS_RDF, "resource", "urn:x")

        assertSerializeFailsWithBadRdf(xmpMeta, SerializeOptions().setUseCanonicalFormat(true))
    }

    /**
     * A struct with an rdf:resource qualifier and attribute fields uses the
     * empty property element form in canonical serialization.
     */
    @Test
    fun testSerializeCanonicalResourceStructWithAttributeFields() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "struct", null, PropertyOptions().setStruct(true))
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1", "a")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "struct", XMPConst.NS_RDF, "resource", "urn:x")

        val serialized = XMPMetaFactory.serializeToString(
            xmpMeta,
            SerializeOptions().setUseCanonicalFormat(true)
        )

        assertTrue(serialized.contains("rdf:resource=\"urn:x\""))
        assertTrue(serialized.contains("dc:field1=\"a\""))
    }

    /**
     * Asserts that serializing throws an XMPException whose cause is the
     * BADRDF error of the RDF writer.
     */
    private fun assertSerializeFailsWithBadRdf(xmpMeta: XMPMeta, options: SerializeOptions) {

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.serializeToString(xmpMeta, options)
        }

        assertEquals(XMPErrorConst.UNKNOWN, ex.errorCode)

        var cause: Throwable? = ex.cause

        while (cause != null) {

            if (cause is XMPException && cause.errorCode == XMPErrorConst.BADRDF)
                return

            cause = cause.cause
        }

        throw AssertionError("Expected a BADRDF cause, got: ${ex.cause}")
    }
}
