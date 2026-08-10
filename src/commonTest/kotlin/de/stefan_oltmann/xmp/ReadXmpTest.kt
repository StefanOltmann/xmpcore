package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.XMPConst.XMP_DC_SUBJECT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Demonstrates how to use the library to read values.
 */
class ReadXmpTest {

    @Test
    fun testReadXmp() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:exif="http://ns.adobe.com/exif/1.0/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                  xmlns:xmpDM="http://ns.adobe.com/xmp/1.0/DynamicMedia/"
                exif:DateTimeOriginal="1980-03-15T08:15:30"
                exif:GPSLatitude="53,13.1635N"
                exif:GPSLongitude="8,14.3797E"
                exif:GPSVersionID="2.3.0.0"
                xmpDM:pick="1"
                xmp:Rating="2">
                <dc:subject>
                  <rdf:Bag>
                    <rdf:li>fox</rdf:li>
                    <rdf:li>swiper</rdf:li>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "1980-03-15T08:15:30",
            actual = xmpMeta.getDateTimeOriginal()
        )

        assertEquals(
            expected = "53,13.1635N",
            actual = xmpMeta.getGpsLatitude()
        )

        assertEquals(
            expected = "8,14.3797E",
            actual = xmpMeta.getGpsLongitude()
        )

        assertEquals(
            expected = "2.3.0.0",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID")
        )

        assertEquals(
            expected = 2,
            actual = xmpMeta.getRating()
        )

        assertTrue(xmpMeta.isFlagged())

        assertEquals(
            expected = 2,
            actual = xmpMeta.countArrayItems(XMPConst.NS_DC, XMP_DC_SUBJECT)
        )

        assertEquals(
            expected = "fox",
            actual = xmpMeta.getPropertyString(XMPConst.NS_DC, "$XMP_DC_SUBJECT[1]")
        )

        assertEquals(
            expected = "swiper",
            actual = xmpMeta.getPropertyString(XMPConst.NS_DC, "$XMP_DC_SUBJECT[2]")
        )

        assertEquals(
            expected = setOf("fox", "swiper"),
            actual = xmpMeta.getKeywords()
        )

        assertNull(xmpMeta.getLocation())
    }

    @Test
    fun testReadXmpWithIptcLocation() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 6.0.0">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                        xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"
                        xmlns:Iptc4xmpCore="http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"
                        xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/"
                        xmlns:exif="http://ns.adobe.com/exif/1.0/"
                        xmlns:dc="http://purl.org/dc/elements/1.1/"
                        photoshop:Country="Deutschland"
                        photoshop:State="Niedersachsen"
                        photoshop:City="Oldenburg"
                        Iptc4xmpCore:Location="Schnurpselstraße 7">
                        <Iptc4xmpExt:LocationShown>
                            <rdf:Bag>
                                <rdf:li>
                                    <rdf:Description
                                        Iptc4xmpExt:CountryName="Deutschland"
                                        Iptc4xmpExt:ProvinceState="Niedersachsen"
                                        Iptc4xmpExt:City="Oldenburg"
                                        Iptc4xmpExt:Sublocation="Schnurpselstraße 7">
                                        <Iptc4xmpExt:LocationName>
                                            <rdf:Alt>
                                                <rdf:li xml:lang="x-default">Example GmbH &amp; Co. KG</rdf:li>
                                            </rdf:Alt>
                                        </Iptc4xmpExt:LocationName>
                                    </rdf:Description>
                                </rdf:li>
                            </rdf:Bag>
                        </Iptc4xmpExt:LocationShown>
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = XMPLocation(
                name = "Example GmbH & Co. KG",
                location = "Schnurpselstraße 7",
                city = "Oldenburg",
                state = "Niedersachsen",
                country = "Deutschland"
            ),
            actual = xmpMeta.getLocation()
        )
    }

    @Test
    fun testReadXmpWithPhotoshopLocation() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="XMP Core 6.0.0">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                        xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/"
                        xmlns:Iptc4xmpCore="http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"
                        xmlns:exif="http://ns.adobe.com/exif/1.0/"
                        xmlns:dc="http://purl.org/dc/elements/1.1/"
                        photoshop:Country="Deutschland"
                        photoshop:State="Niedersachsen"
                        photoshop:City="Oldenburg"
                        Iptc4xmpCore:Location="Schnurpselstraße 7">
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = XMPLocation(
                name = null,
                location = "Schnurpselstraße 7",
                city = "Oldenburg",
                state = "Niedersachsen",
                country = "Deutschland"
            ),
            actual = xmpMeta.getLocation()
        )
    }

    @Test
    fun readXmpWithTitleAndDescription() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="${XMPVersionInfo.VERSION_MESSAGE}">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:description>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Vögel auf dem Wasser.</rdf:li>
                    </rdf:Alt>
                  </dc:description>
                  <dc:title>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Süße Vögelchen</rdf:li>
                    </rdf:Alt>
                  </dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Süße Vögelchen",
            actual = xmpMeta.getTitle()
        )

        assertEquals(
            expected = "Vögel auf dem Wasser.",
            actual = xmpMeta.getDescription()
        )
    }

    /**
     * The xpacket processing instruction must survive parsing.
     */
    @Test
    fun testPacketHeaderSurvivesParse() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
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
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val packetHeader = xmpMeta.getPacketHeader()

        assertNotNull(packetHeader)
        assertTrue(packetHeader.contains("id=\"W5M0MpCehiHzreSzNTczkc9d\""))
    }

    /**
     * Namespace declarations on ancestor elements must be honored.
     */
    @Test
    fun testParseWithOuterNamespaceDeclaration() {

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about="">
                  <dc:title>
                    <rdf:Alt>
                      <rdf:li xml:lang="x-default">Titel</rdf:li>
                    </rdf:Alt>
                  </dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Titel",
            actual = xmpMeta.getTitle()
        )
    }

    /**
     * A different prefix for the RDF namespace must be accepted.
     */
    @Test
    fun testParseWithAlternativeRdfPrefix() {

        /* language=XML */
        val testXmp = """
            <foo:RDF xmlns:foo="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <foo:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </foo:Description>
            </foo:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Titel",
            actual = xmpMeta.getTitle()
        )
    }

    /**
     * An rdf:ID attribute on a literal property element must not prevent parsing.
     */
    @Test
    fun testParseWithRdfIdAttributeOnLiteralProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:ID="title1">Some title</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Some title",
            actual = xmpMeta.getTitle()
        )
    }

    /**
     * An rdf:ID attribute combined with xml:lang on a literal property element must not
     * prevent parsing.
     */
    @Test
    fun testParseWithRdfIdAndLangOnLiteralProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title xml:lang="en" rdf:ID="title1">Some title</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Some title",
            actual = xmpMeta.getTitle()
        )
    }

    /**
     * A literal property element with an `rdf:datatype` attribute keeps its value.
     */
    @Test
    fun testParseWithDatatypeOnLiteralProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Some title</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Some title",
            actual = xmpMeta.getTitle()
        )

        assertEquals(
            expected = "Some title",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )
    }

    /**
     * A literal property element with `rdf:datatype` and an element child is invalid RDF.
     */
    @Test
    fun testParseWithDatatypeAndElementChildThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
                  <rdf:li>child</rdf:li>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }
}
