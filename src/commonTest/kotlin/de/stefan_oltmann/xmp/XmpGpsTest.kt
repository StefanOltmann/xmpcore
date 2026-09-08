package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests [XmpGps]: parsing of the DDM and DMS values the XMP specification and broken tools
 * write, rendering back into DDM with byte-identical output on every platform, and the
 * `XMPMeta` convenience accessors.
 */
class XmpGpsTest {

    /**
     * Typical DDM values, like the ones ExifTool and this library write, parse into decimal
     * degrees.
     */
    @Test
    fun testParsesDdmValues() {

        val gps = XmpGps.parse("53,13.1635N", "8,14.3797E")!!

        assertEquals(53.219391666666666, gps.latitude, absoluteTolerance = 1.0E-12)

        assertEquals(8.239661666666666, gps.longitude, absoluteTolerance = 1.0E-12)
    }

    /**
     * The hemisphere letters determine the sign, in upper and lower case.
     */
    @Test
    fun testParsesHemispheresAndLetterCase() {

        val gps = XmpGps.parse("53,30.0s", "8,30.0w")!!

        assertEquals(-53.5, gps.latitude, absoluteTolerance = 1.0E-12)

        assertEquals(-8.5, gps.longitude, absoluteTolerance = 1.0E-12)
    }

    /**
     * The DMS form with seconds is accepted too.
     */
    @Test
    fun testParsesDmsWithSeconds() {

        val gps = XmpGps.parse("53,13,30N", "8,14,30E")!!

        assertEquals(53.225, gps.latitude, absoluteTolerance = 1.0E-12)

        assertEquals(8.241666666666667, gps.longitude, absoluteTolerance = 1.0E-12)
    }

    /**
     * Corrupted values are rejected instead of producing a bogus position.
     */
    @Test
    fun testParsesRejectsInvalidValues() {

        assertNull(XmpGps.parse(null, "8,14.3797E"))

        assertNull(XmpGps.parse("53,13.1635N", ""))

        assertNull(XmpGps.parse("  ", "8,14.3797E"))

        assertNull(XmpGps.parse("53,13.1635", "8,14.3797E"))

        assertNull(XmpGps.parse("53,60.0N", "8,14.3797E"))

        assertNull(XmpGps.parse("53,13.1635N", "8,60.0E"))

        assertNull(XmpGps.parse("53,13,60N", "8,14.3797E"))

        assertNull(XmpGps.parse("not-a-coordinate", "8,14.3797E"))
    }

    /**
     * Coordinates outside the valid sphere range are rejected, because corrupt XMP must not
     * flow into the position.
     */
    @Test
    fun testParsesRejectsOutOfRangeCoordinates() {

        assertNull(XmpGps.parse("90,0.1N", "8,14.3797E"))

        assertNull(XmpGps.parse("53,13.1635N", "180,0.1E"))
    }

    /**
     * The constructor rejects out-of-range coordinates as a programming error.
     */
    @Test
    fun testConstructorRejectsOutOfRangeCoordinates() {

        assertThrowsIllegalArgument(90.5, 0.0)

        assertThrowsIllegalArgument(0.0, -180.5)

        assertThrowsIllegalArgument(Double.NaN, 0.0)
    }

    /**
     * Rendering produces the DDM form with four minute fraction digits and the hemisphere
     * letter, mirroring the golden values of the real-world test data.
     */
    @Test
    fun testRendersDdmValues() {

        assertEquals(
            "53,13.1635N",
            XmpGps(53.219392, 8.239662).toLatitudeDdm()
        )

        assertEquals(
            "8,14.3797E",
            XmpGps(53.219392, 8.239662).toLongitudeDdm()
        )
    }

    /**
     * Whole degrees, the poles and the date line render without fraction noise, and a minute
     * carry of a rounding step must round over into the degrees instead of writing an invalid
     * minute of 60.0000.
     */
    @Test
    fun testRendersEdgeValues() {

        assertEquals("0,0.0N", XmpGps(0.0, 0.0).toLatitudeDdm())

        assertEquals("0,0.0E", XmpGps(0.0, 0.0).toLongitudeDdm())

        assertEquals("90,0.0N", XmpGps(90.0, 0.0).toLatitudeDdm())

        assertEquals("90,0.0S", XmpGps(-90.0, 0.0).toLatitudeDdm())

        assertEquals("180,0.0E", XmpGps(0.0, 180.0).toLongitudeDdm())

        assertEquals("180,0.0W", XmpGps(0.0, -180.0).toLongitudeDdm())

        /* 53.9999995 degrees: the minutes round to 60.0000 and carry into the degrees. */
        assertEquals("54,0.0N", XmpGps(53.9999995, 0.0).toLatitudeDdm())
    }

    /**
     * Minute values below 0.001 render in exponent notation like the JVM's double rendering,
     * which the renderer must reproduce byte-identically on every platform.
     */
    @Test
    fun testRendersSmallMinutesInExponentNotation() {

        assertEquals("0,5.0E-4N", XmpGps(0.0005 / 60.0, 0.0).toLatitudeDdm())
    }

    /**
     * Parsing the rendered values returns the same position within the DDM precision of four
     * minute fraction digits.
     */
    @Test
    fun testRenderParseRoundTrip() {

        val positions = listOf(
            XmpGps(53.219392, 8.239662),
            XmpGps(-33.868820, 151.209290),
            XmpGps(0.0, 0.0),
            XmpGps(90.0, -180.0),
            XmpGps(-90.0, 180.0)
        )

        for (position in positions) {

            val parsed = XmpGps.parse(position.toLatitudeDdm(), position.toLongitudeDdm())!!

            /* The DDM precision of four minute fraction digits is about 0.2 m. */
            assertEquals(position.latitude, parsed.latitude, absoluteTolerance = 2.0E-6)

            assertEquals(position.longitude, parsed.longitude, absoluteTolerance = 2.0E-6)
        }
    }

    /**
     * The convenience accessor parses and range validates the property values.
     */
    @Test
    fun testXmpMetaGpsCoordinatesRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getGpsCoordinates())

        xmpMeta.setGpsCoordinates(XmpGps(53.219392, 8.239662))

        assertEquals("53,13.1635N", xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSLatitude"))

        assertEquals("8,14.3797E", xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSLongitude"))

        assertEquals(
            "2.3.0.0",
            xmpMeta.getPropertyString(XMPConst.NS_EXIF, "GPSVersionID")
        )

        val gps = xmpMeta.getGpsCoordinates()!!

        assertEquals(53.219391666666666, gps.latitude, absoluteTolerance = 1.0E-9)

        assertEquals(8.239661666666666, gps.longitude, absoluteTolerance = 1.0E-9)
    }

    /**
     * Corrupted or out-of-range property values read back as null.
     */
    @Test
    fun testXmpMetaGpsCoordinatesWithCorruptValues() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSLatitude", "53,75.0N")

        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSLongitude", "8,14.3797E")

        assertNull(xmpMeta.getGpsCoordinates())

        xmpMeta.setProperty(XMPConst.NS_EXIF, "GPSLatitude", "91,0.0N")

        assertNull(xmpMeta.getGpsCoordinates())
    }

    private fun assertThrowsIllegalArgument(latitude: Double, longitude: Double) {

        assertFailsWith<IllegalArgumentException> {
            XmpGps(latitude, longitude)
        }
    }
}
