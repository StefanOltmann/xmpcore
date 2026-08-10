package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.ParseOptions
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the convenience methods of [XMPMeta] for commonly used fields.
 */
class XMPMetaConvenienceTest {

    /**
     * The rating convenience methods read and write the xmp:Rating property.
     */
    @Test
    fun testRatingRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getRating())

        xmpMeta.setRating(3)

        assertEquals(3, xmpMeta.getRating())
    }

    /**
     * The orientation convenience methods read and write the tiff:Orientation property.
     */
    @Test
    fun testOrientationRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getOrientation())

        xmpMeta.setOrientation(6)

        assertEquals(6, xmpMeta.getOrientation())
    }

    /**
     * The date time original convenience methods write and delete the property.
     */
    @Test
    fun testDateTimeOriginalRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setDateTimeOriginal("2023-07-07T13:37:42")

        assertEquals("2023-07-07T13:37:42", xmpMeta.getDateTimeOriginal())

        xmpMeta.deleteDateTimeOriginal()

        assertNull(xmpMeta.getDateTimeOriginal())
    }

    /**
     * Setting GPS coordinates writes the mandatory GPS version id too.
     */
    @Test
    fun testSetGpsCoordinates() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setGpsCoordinates("53,13.1635N", "8,14.3797E")

        assertEquals("53,13.1635N", xmpMeta.getGpsLatitude())
        assertEquals("8,14.3797E", xmpMeta.getGpsLongitude())
        assertEquals(
            expected = "2.3.0.0",
            actual = xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID")
        )
    }

    /**
     * Deleting GPS coordinates removes all three properties.
     */
    @Test
    fun testDeleteGpsCoordinates() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setGpsCoordinates("53,13.1635N", "8,14.3797E")

        xmpMeta.deleteGpsCoordinates()

        assertNull(xmpMeta.getGpsLatitude())
        assertNull(xmpMeta.getGpsLongitude())
        assertNull(xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID"))
    }

    /**
     * Setting the flagged state writes all known flag schemas.
     */
    @Test
    fun testSetFlaggedWritesAllSchemas() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setFlagged(true)

        assertTrue(xmpMeta.isFlagged())
        assertEquals("1", xmpMeta.getPropertyString(XMPConst.NS_DM, "pick"))
        assertEquals("True", xmpMeta.getPropertyString(XMPConst.NS_ACDSEE, "tagged"))
        assertEquals("true", xmpMeta.getPropertyString(XMPConst.NS_MYLIO, "flag"))
        assertEquals("True", xmpMeta.getPropertyString(XMPConst.NS_NARRATIVE, "Tagged"))
    }

    /**
     * Unsetting the flagged state writes the false values.
     */
    @Test
    fun testSetFlaggedFalse() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setFlagged(true)
        xmpMeta.setFlagged(false)

        assertFalse(xmpMeta.isFlagged())
        assertEquals("0", xmpMeta.getPropertyString(XMPConst.NS_DM, "pick"))
    }

    /**
     * A single positive flag in any schema makes [XMPMeta.isFlagged] true.
     */
    @Test
    fun testIsFlaggedForSingleSchema() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyBoolean(XMPConst.NS_ACDSEE, "tagged", true)

        assertTrue(xmpMeta.isFlagged())
    }

    /**
     * The keywords are stored as a dc:subject array and read back.
     */
    @Test
    fun testKeywordsRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(emptySet(), xmpMeta.getKeywords())

        xmpMeta.setKeywords(setOf("bird", "cat", "dog"))

        assertEquals(setOf("bird", "cat", "dog"), xmpMeta.getKeywords())
        assertEquals(3, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * Setting an empty keyword set deletes the array.
     */
    @Test
    fun testSetEmptyKeywordsDeletesArray() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setKeywords(setOf("bird"))
        xmpMeta.setKeywords(emptySet())

        assertEquals(emptySet(), xmpMeta.getKeywords())
        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "subject"))
    }

    /**
     * ACDSee keywords are read from the acdsee namespace.
     */
    @Test
    fun testGetAcdSeeKeywords() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(emptySet(), xmpMeta.getAcdSeeKeywords())

        xmpMeta.appendArrayItem(
            XMPConst.NS_ACDSEE,
            "keywords",
            PropertyOptions().setArray(true),
            "k1"
        )

        assertEquals(setOf("k1"), xmpMeta.getAcdSeeKeywords())
    }

    /**
     * An empty ACDSee keyword array reads back as an empty set.
     */
    @Test
    fun testGetAcdSeeKeywordsWithEmptyArray() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(
            XMPConst.NS_ACDSEE,
            "keywords",
            null,
            PropertyOptions().setArray(true)
        )

        assertEquals(emptySet(), xmpMeta.getAcdSeeKeywords())
    }

    /**
     * An empty region list reads back as an empty face map.
     */
    @Test
    fun testGetFacesWithEmptyRegionList() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:RegionList>
                    <rdf:Bag/>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(emptyMap(), xmpMeta.getFaces())
    }

    /**
     * The faces are stored as mwg-rs regions and read back.
     */
    @Test
    fun testFacesRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(emptyMap(), xmpMeta.getFaces())

        val faces = mapOf(
            "Face A" to XMPRegionArea(0.1, 0.2, 0.3, 0.4),
            "Face B" to XMPRegionArea(0.5, 0.6, 0.7, 0.8)
        )

        xmpMeta.setFaces(faces, widthPx = 1500, heightPx = 1000)

        assertEquals(faces, xmpMeta.getFaces())
    }

    /**
     * Setting an empty face map deletes the regions.
     */
    @Test
    fun testSetEmptyFacesDeletesRegions() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setFaces(mapOf("Face A" to XMPRegionArea(0.1, 0.2, 0.3, 0.4)), 1500, 1000)
        xmpMeta.setFaces(emptyMap(), 1500, 1000)

        assertEquals(emptyMap(), xmpMeta.getFaces())
    }

    /**
     * Regions that are no faces are skipped when reading.
     */
    @Test
    fun testGetFacesSkipsNonFaceRegions() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Doggy"
                          mwg-rs:Type="Pet">
                        <mwg-rs:Area
                          stArea:h="0.05"
                          stArea:unit="normalized"
                          stArea:w="0.03"
                          stArea:x="0.2"
                          stArea:y="0.3"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="NoArea"
                          mwg-rs:Type="Face"/>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(emptyMap(), xmpMeta.getFaces())
    }

    /**
     * The persons in image are stored as an Iptc4xmpExt array and read back.
     */
    @Test
    fun testPersonsInImageRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(emptySet(), xmpMeta.getPersonsInImage())

        xmpMeta.setPersonsInImage(setOf("Anna", "Ben"))

        assertEquals(setOf("Anna", "Ben"), xmpMeta.getPersonsInImage())

        xmpMeta.setPersonsInImage(emptySet())

        assertEquals(emptySet(), xmpMeta.getPersonsInImage())
        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_IPTC_EXT, "PersonInImage"))
    }

    /**
     * A missing location reads back as null.
     */
    @Test
    fun testGetLocationOnEmptyXmp() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getLocation())
    }

    /**
     * Missing Iptc4xmpExt fields fall back to the photoshop fields.
     */
    @Test
    fun testGetLocationFallsBackToPhotoshopFields() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/"
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <Iptc4xmpExt:LocationShown>
                  <rdf:Bag>
                    <rdf:li>
                      <rdf:Description Iptc4xmpExt:Sublocation="Schnurpselstraße 7"/>
                    </rdf:li>
                  </rdf:Bag>
                </Iptc4xmpExt:LocationShown>
                <photoshop:City>Oldenburg</photoshop:City>
                <photoshop:State>Niedersachsen</photoshop:State>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = XMPLocation(
                name = null,
                location = "Schnurpselstraße 7",
                city = "Oldenburg",
                state = "Niedersachsen",
                country = null
            ),
            actual = xmpMeta.getLocation()
        )
    }

    /**
     * Setting a partial location writes only the provided fields.
     */
    @Test
    fun testSetLocationWithPartialFields() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setLocation(
            XMPLocation(
                name = null,
                location = null,
                city = "Oldenburg",
                state = null,
                country = null
            )
        )

        assertEquals("Oldenburg", xmpMeta.getPropertyString(XMPConst.NS_PHOTOSHOP, "City"))
        assertNull(xmpMeta.getPropertyString(XMPConst.NS_PHOTOSHOP, "State"))

        assertEquals(
            expected = XMPLocation(null, null, "Oldenburg", null, null),
            actual = xmpMeta.getLocation()
        )
    }

    /**
     * Setting a completely empty location clears all location fields.
     */
    @Test
    fun testSetEmptyLocationClearsFields() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setLocation(XMPLocation("Name", "Street", "City", "State", "Country"))
        xmpMeta.setLocation(XMPLocation(null, null, null, null, null))

        assertNull(xmpMeta.getLocation())
    }

    /**
     * The title convenience methods read and write the dc:title alt array.
     */
    @Test
    fun testTitleRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getTitle())

        xmpMeta.setTitle("Süße Kätzchen")

        assertEquals("Süße Kätzchen", xmpMeta.getTitle())

        xmpMeta.setTitle(null)

        assertNull(xmpMeta.getTitle())
        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))
    }

    /**
     * The description convenience methods read and write the dc:description alt array.
     */
    @Test
    fun testDescriptionRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getDescription())

        xmpMeta.setDescription("Eine Beschreibung")

        assertEquals("Eine Beschreibung", xmpMeta.getDescription())

        xmpMeta.setDescription(null)

        assertNull(xmpMeta.getDescription())
    }

    /**
     * The object name defaults to the empty string and is read from rdf:about.
     */
    @Test
    fun testObjectName() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals("", xmpMeta.getObjectName())

        xmpMeta.setObjectName("http://example.org/")

        assertEquals("http://example.org/", xmpMeta.getObjectName())

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="abc"/>
            </rdf:RDF>
        """.trimIndent()

        assertEquals("abc", XMPMetaFactory.parseFromString(testXmp).getObjectName())
    }

    /**
     * The packet header is only available after parsing a wrapped packet.
     */
    @Test
    fun testPacketHeader() {

        assertNull(XMPMetaFactory.create().getPacketHeader())

        /* language=XML */
        val testXmp = """
            <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""/>
              </rdf:RDF>
            </x:xmpmeta>
            <?xpacket end="w"?>
        """.trimIndent()

        assertTrue(XMPMetaFactory.parseFromString(testXmp).getPacketHeader()!!.contains("W5M0MpCehiHzreSzNTczkc9d"))

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPacketHeader("custom header")

        assertEquals("custom header", xmpMeta.getPacketHeader())
    }

    /**
     * Sorting orders the schemas by prefix and the properties by name.
     */
    @Test
    fun testSortOrdersTree() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "zebra", "1")
        xmpMeta.setProperty(XMPConst.NS_XMP, "mango", "1")
        xmpMeta.setProperty(XMPConst.NS_DC, "apple", "1")

        xmpMeta.sort()

        val paths = mutableListOf<String>()

        val iterator = xmpMeta.iterator()

        while (iterator.hasNext())
            paths.add(iterator.next().getPath())

        assertEquals(
            expected = listOf("", "dc:apple", "dc:zebra", "", "xmp:mango"),
            actual = paths
        )
    }

    /**
     * Normalizing an XMP parsed with OMIT_NORMALIZATION makes the
     * dc:title array readable via the convenience methods.
     */
    @Test
    fun testNormalizeAfterOmitNormalization() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Titel</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(
            testXmp,
            ParseOptions().setOmitNormalization(true)
        )

        assertNull(xmpMeta.getTitle())

        xmpMeta.normalize(ParseOptions())

        assertEquals("Titel", xmpMeta.getTitle())
    }
}
