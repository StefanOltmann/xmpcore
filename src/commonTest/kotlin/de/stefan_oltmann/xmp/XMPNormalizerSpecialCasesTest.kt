package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the data model repairs of [de.stefan_oltmann.xmp.internal.XMPNormalizer] that fix
 * known defects of writing tools: the date-less exif:GPSTimeStamp and the xmpDM:copyright
 * migration, ported from the Adobe XMPCore original.
 */
class XMPNormalizerSpecialCasesTest {

    /**
     * A GPS time stamp without calendar part receives the calendar part of
     * exif:DateTimeOriginal.
     */
    @Test
    fun testGpsTimeStampWithoutDateGetsDateFromDateTimeOriginal() {

        val xmpMeta = parse(
            """
                <exif:DateTimeOriginal>2020-05-01T08:15:30</exif:DateTimeOriginal>
                <exif:GPSTimeStamp>0000-00-00T10:20:30</exif:GPSTimeStamp>
            """.trimIndent()
        )

        assertEquals(
            "2020-05-01T10:20:30",
            xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSTimeStamp")
        )
    }

    /**
     * Without DateTimeOriginal the calendar part comes from exif:DateTimeDigitized.
     */
    @Test
    fun testGpsTimeStampWithoutDateGetsDateFromDateTimeDigitized() {

        val xmpMeta = parse(
            """
                <exif:DateTimeDigitized>2019-12-24T22:00:00</exif:DateTimeDigitized>
                <exif:GPSTimeStamp>0000-00-00T05:00:00+02:00</exif:GPSTimeStamp>
            """.trimIndent()
        )

        assertEquals(
            "2019-12-24T05:00:00+02:00",
            xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSTimeStamp")
        )
    }

    /**
     * A GPS time stamp with a real calendar part is left alone.
     */
    @Test
    fun testGpsTimeStampWithRealDateStaysUnchanged() {

        val xmpMeta = parse(
            """
                <exif:DateTimeOriginal>2020-05-01T08:15:30</exif:DateTimeOriginal>
                <exif:GPSTimeStamp>2021-06-15T10:20:30</exif:GPSTimeStamp>
            """.trimIndent()
        )

        assertEquals(
            "2021-06-15T10:20:30",
            xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSTimeStamp")
        )
    }

    /**
     * Without a second date the defective GPS time stamp is left unchanged.
     */
    @Test
    fun testGpsTimeStampWithoutOtherDateStaysUnchanged() {

        val xmpMeta = parse(
            """
                <exif:GPSTimeStamp>0000-00-00T10:20:30</exif:GPSTimeStamp>
            """.trimIndent()
        )

        assertEquals(
            "0000-00-00T10:20:30",
            xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSTimeStamp")
        )
    }

    /**
     * Without dc:rights the audio copyright migrates into a new x-default value with the
     * leading empty line the Adobe normalizer writes.
     */
    @Test
    fun testAudioCopyrightMigratesIntoNewDcRights() {

        val xmpMeta = parse(
            """
                <xmpDM:copyright>Acme Sound</xmpDM:copyright>
            """.trimIndent()
        )

        assertEquals("\n\nAcme Sound", readRightsXDefault(xmpMeta))

        assertNull(xmpMeta.getPropertyString(XMPConst.NS_DM, "copyright"))
    }

    /**
     * An existing x-default value keeps the copyright as appended second line.
     */
    @Test
    fun testAudioCopyrightAppendsToExistingDcRights() {

        val xmpMeta = parse(
            """
                <dc:rights>
                <rdf:Alt>
                  <rdf:li xml:lang="x-default">Some Author</rdf:li>
                </rdf:Alt>
                </dc:rights>
                <xmpDM:copyright>Acme Sound</xmpDM:copyright>
            """.trimIndent()
        )

        assertEquals(
            "Some Author\n\nAcme Sound",
            readRightsXDefault(xmpMeta)
        )

        assertNull(xmpMeta.getPropertyString(XMPConst.NS_DM, "copyright"))
    }

    /**
     * A x-default value that already carries a copyright tail gets the tail replaced.
     */
    @Test
    fun testAudioCopyrightReplacesExistingTail() {

        val xmpMeta = parse(
            """
                <dc:rights>
                <rdf:Alt>
                  <rdf:li xml:lang="x-default">Some Author&#10;&#10;Old Sound</rdf:li>
                </rdf:Alt>
                </dc:rights>
                <xmpDM:copyright>Acme Sound</xmpDM:copyright>
            """.trimIndent()
        )

        assertEquals(
            "Some Author\n\nAcme Sound",
            readRightsXDefault(xmpMeta)
        )
    }

    /**
     * An identical copyright tail is kept as-is.
     */
    @Test
    fun testAudioCopyrightKeepsIdenticalTail() {

        val xmpMeta = parse(
            """
                <dc:rights>
                <rdf:Alt>
                  <rdf:li xml:lang="x-default">Some Author&#10;&#10;Acme Sound</rdf:li>
                </rdf:Alt>
                </dc:rights>
                <xmpDM:copyright>Acme Sound</xmpDM:copyright>
            """.trimIndent()
        )

        assertEquals(
            "Some Author\n\nAcme Sound",
            readRightsXDefault(xmpMeta)
        )
    }

    /**
     * Reads the x-default value of dc:rights, the slot the audio copyright migrates into.
     */
    private fun readRightsXDefault(xmpMeta: XMPMeta): String? {

        val property = xmpMeta.getLocalizedText(XMPConst.NS_DC, "rights", "", XMPConst.X_DEFAULT)

        return property?.getValue()
    }

    private fun parse(properties: String): XMPMeta {

        val rdf = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:exif="http://ns.adobe.com/exif/1.0/"
                  xmlns:xmpDM="http://ns.adobe.com/xmp/1.0/DynamicMedia/">
            $properties
                </rdf:Description>
                </rdf:RDF>
        """.trimIndent()

        return XMPMetaFactory.parseFromString(rdf)
    }
}
