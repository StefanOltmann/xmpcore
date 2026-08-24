package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the RDF parser features of the XMP data model.
 */
class XMPParseFeatureTest {

    /**
     * A typed node element sets the rdf:type qualifier.
     */
    @Test
    fun testTypedNodeGetsRdfTypeQualifier() {

        val typeNamespace = "http://example.org/xmpcore-typed/"

        XMPSchemaRegistry.registerNamespace(typeNamespace, "typed")

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:typed="$typeNamespace">
                <xmp:thing>
                  <typed:MyType typed:field="v"/>
                </xmp:thing>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "$typeNamespace:MyType",
            actual = checkNotNull(xmpMeta.getQualifier(XMPConst.NS_XMP, "thing", XMPConst.NS_RDF, "type")).getValue()
        )
        assertEquals(
            expected = "v",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "thing/typed:field")
        )
    }

    /**
     * An empty property element with rdf:resource becomes a URI property.
     */
    @Test
    fun testUriPropertyViaRdfResource() {

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

        val property = checkNotNull(xmpMeta.getProperty(XMPConst.NS_XMP, "Webpage"))

        assertEquals("http://example.org/", property.getValue())
        assertTrue(property.getOptions().isURI())
    }

    /**
     * An empty property element with several attributes becomes a struct.
     */
    @Test
    fun testStructFromEmptyPropertyAttributes() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:struct dc:field1="a" dc:field2="b"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "a",
            actual = checkNotNull(
                xmpMeta.getStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1")
            ).getValue()
        )
        assertEquals(
            expected = "b",
            actual = checkNotNull(
                xmpMeta.getStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field2")
            ).getValue()
        )
    }

    /**
     * An element with rdf:parseType="Resource" becomes a struct.
     */
    @Test
    fun testStructViaParseTypeResource() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <xmp:struct rdf:parseType="Resource">
                  <dc:field1>a</dc:field1>
                </xmp:struct>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "a",
            actual = checkNotNull(
                xmpMeta.getStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "field1")
            ).getValue()
        )
    }

    /**
     * Comments inside the description are ignored.
     */
    @Test
    fun testCommentsAreSkipped() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <!-- Ein Kommentar -->
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An unknown namespace in the document is registered with its prefix.
     */
    @Test
    fun testUnknownNamespaceRegisteredWithDocumentPrefix() {

        val unknownNamespace = "http://example.org/xmpcore-unknown/"

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:unkn="$unknownNamespace">
                <unkn:bar>value</unkn:bar>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("value", xmpMeta.getPropertyString(unknownNamespace, "bar"))
        assertEquals("unkn:", XMPSchemaRegistry.getNamespacePrefix(unknownNamespace))
    }

    /**
     * The deprecated Dublin Core namespace is converted to the current one.
     */
    @Test
    fun testLegacyDcNamespaceConverted() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/1.1/">
                <dc:title>Titel</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * Language qualifier values are normalized on parse.
     */
    @Test
    fun testLanguageAttributeNormalizedOnParse() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="DE-de">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de-DE")).getValue()
        )
    }

    /**
     * Old iX:changes punchcard chaff is skipped.
     */
    @Test
    fun testIXChangesSkipped() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:iX="http://ns.adobe.com/iX/1.0/">
                <iX:changes>
                  <rdf:Description/>
                </iX:changes>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_IX, "changes"))
    }

    /**
     * The rdf:nodeID attribute is ignored.
     */
    @Test
    fun testNodeIdIgnored() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:nodeID="n1"
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An empty property element reads back with an empty value.
     */
    @Test
    fun testEmptyPropertyElementWithEmptyValue() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getPropertyString(XMPConst.NS_XMP, "prop"))
    }

    /**
     * An rdf:li with an rdf:about attribute is parsed as an empty item.
     */
    @Test
    fun testRdfLiWithAboutAttribute() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:subject>
                  <rdf:Bag>
                    <rdf:li rdf:about=""/>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("", checkNotNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)).getValue())
    }

    /**
     * An unprefixed about attribute is recognized as rdf:about.
     */
    @Test
    fun testUnprefixedAboutAttribute() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description about="object-name"/>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "object-name",
            actual = XMPMetaFactory.parseFromString(testXmp).getObjectName()
        )
    }

    /**
     * Numbered rdf:_N array items are accepted.
     */
    @Test
    fun testNumberedRdfArrayItems() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:subject>
                  <rdf:Bag>
                    <rdf:_1>fox</rdf:_1>
                    <rdf:_2>swiper</rdf:_2>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("swiper", checkNotNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)).getValue())
    }

    /**
     * Namespace declarations on property elements are removed before parsing.
     */
    @Test
    fun testXmlnsDeclarationOnPropertyElement() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An rdf:ID attribute on a resource property element is ignored.
     */
    @Test
    fun testRdfIdOnResourceProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:ID="title1">
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An XMP document inside a wrapper element before the xmpmeta is found.
     */
    @Test
    fun testParseWithNonXmpElementBeforeXmpmeta() {

        /* language=XML */
        val testXmp = """
            <wrapper>
              <meta/>
              <x:xmpmeta xmlns:x="adobe:ns:meta/">
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
            </wrapper>
        """.trimIndent()

        assertEquals(
            expected = "Titel",
            actual = XMPMetaFactory.parseFromString(testXmp).getTitle()
        )
    }

    /**
     * An unprefixed element in a default namespace gets the default prefix.
     */
    @Test
    fun testUnprefixedElementGetsDefaultPrefix() {

        val defaultNamespace = "http://example.org/xmpcore-default/"

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:struct>
                  <rdf:Description>
                    <elem xmlns="$defaultNamespace"/>
                  </rdf:Description>
                </xmp:struct>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("_dflt:", XMPSchemaRegistry.getNamespacePrefix(defaultNamespace))
        assertEquals(
            expected = "",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "struct/_dflt:elem")
        )
    }

    /**
     * A slightly corrupted file with a duplicated property is parsed instead
     * of rejected. The last occurrence wins like in ExifTool.
     */
    @Test
    fun testDuplicatedPropertyKeepsLastValue() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <photoshop:City>Oldenburg</photoshop:City>
                <photoshop:City>Berlin</photoshop:City>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("Berlin", xmpMeta.getPropertyString(XMPConst.NS_PHOTOSHOP, "City"))
    }

    /**
     * A duplicated field inside one struct keeps the last value.
     */
    @Test
    fun testDuplicatedStructFieldKeepsLastValue() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:RegionList>
                    <rdf:Seq>
                      <rdf:li rdf:parseType="Resource">
                        <mwg-rs:Name>First</mwg-rs:Name>
                        <mwg-rs:Name>Second</mwg-rs:Name>
                        <mwg-rs:Type>Face</mwg-rs:Type>
                      </rdf:li>
                    </rdf:Seq>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Second",
            actual = xmpMeta.getPropertyString(
                XMPConst.NS_MWG_RS,
                "Regions/mwg-rs:RegionList[1]/mwg-rs:Name"
            )
        )
    }
}
