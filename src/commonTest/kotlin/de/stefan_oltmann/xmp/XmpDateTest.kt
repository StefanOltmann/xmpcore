package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the ISO 8601 parser of [XmpDate] against the variants the XMP specification allows
 * and the broken ones broken tools write, including the round trip through [XmpDate.toString].
 */
class XmpDateTest {

    /**
     * A full date-time with a negative UTC offset parses into its components.
     */
    @Test
    fun testParsesFullDateTimeWithNegativeOffset() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, -300),
            actual = XmpDate.parse("2023-05-12T18:04:07-05:00")
        )
    }

    /**
     * The UTC designator parses as a zero offset, in upper and lower case.
     */
    @Test
    fun testParsesUtcDesignator() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, 0),
            actual = XmpDate.parse("2023-05-12T18:04:07Z")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, 0),
            actual = XmpDate.parse("2023-05-12T18:04:07z")
        )
    }

    /**
     * Offsets may omit the colon or the minutes, like broken tools write them.
     */
    @Test
    fun testParsesOffsetsWithoutColonAndMinutes() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, 330),
            actual = XmpDate.parse("2023-05-12T18:04:07+0530")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, 300),
            actual = XmpDate.parse("2023-05-12T18:04:07+05")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, -330),
            actual = XmpDate.parse("2023-05-12T18:04:07-05:30")
        )
    }

    /**
     * Fractional seconds land in the nanosecond field, padded and truncated to nine digits.
     */
    @Test
    fun testParsesFractionalSeconds() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 500_000_000, null),
            actual = XmpDate.parse("2023-05-12T18:04:07.5")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 123_456_789, null),
            actual = XmpDate.parse("2023-05-12T18:04:07.123456789")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 123_456_789, null),
            actual = XmpDate.parse("2023-05-12T18:04:07.1234567891")
        )
    }

    /**
     * Reduced precision values parse with the absent parts reported as zero.
     */
    @Test
    fun testParsesReducedPrecision() {

        assertEquals(
            expected = XmpDate(2023, 0, 0, 0, 0, 0, 0, null),
            actual = XmpDate.parse("2023")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 0, 0, 0, 0, 0, null),
            actual = XmpDate.parse("2023-05")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 0, 0, 0, 0, null),
            actual = XmpDate.parse("2023-05-12")
        )

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 0, 0, null),
            actual = XmpDate.parse("2023-05-12T18:04")
        )
    }

    /**
     * A space is accepted as the time separator, because broken tools write it.
     */
    @Test
    fun testParsesSpaceSeparator() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 18, 4, 7, 0, null),
            actual = XmpDate.parse("2023-05-12 18:04:07")
        )
    }

    /**
     * Whitespace around the value is ignored.
     */
    @Test
    fun testParsesValueWithSurroundingWhitespace() {

        assertEquals(
            expected = XmpDate(2023, 5, 12, 0, 0, 0, 0, null),
            actual = XmpDate.parse("  2023-05-12  ")
        )
    }

    /**
     * Garbage values and range violations return null instead of throwing.
     */
    @Test
    fun testReturnsNullForInvalidValues() {

        assertNull(XmpDate.parse(null))
        assertNull(XmpDate.parse(""))
        assertNull(XmpDate.parse("not-a-date"))
        assertNull(XmpDate.parse("20-05-12"))
        assertNull(XmpDate.parse("2023-13-01"))
        assertNull(XmpDate.parse("2023-00-01"))
        assertNull(XmpDate.parse("2023-05-00"))
        assertNull(XmpDate.parse("2023-05-32"))
        assertNull(XmpDate.parse("2023-02-29"))
        assertNull(XmpDate.parse("1900-02-29"))
        assertNull(XmpDate.parse("2023-05-12T24:00:00"))
        assertNull(XmpDate.parse("2023-05-12T18:60:00"))
        assertNull(XmpDate.parse("2023-05-12T18:04:60"))
        assertNull(XmpDate.parse("2023-05-12T18"))
        assertNull(XmpDate.parse("2023-05-12T18:04:00X"))
        assertNull(XmpDate.parse("2023-05-12junk"))
        assertNull(XmpDate.parse("2023-05-12T18:04:00+05:"))
    }

    /**
     * Leap years are handled for February, including the century rules.
     */
    @Test
    fun testParsesLeapDays() {

        assertEquals(
            expected = XmpDate(2024, 2, 29, 0, 0, 0, 0, null),
            actual = XmpDate.parse("2024-02-29")
        )

        assertEquals(
            expected = XmpDate(2000, 2, 29, 0, 0, 0, 0, null),
            actual = XmpDate.parse("2000-02-29")
        )
    }

    /**
     * The rendered form is valid ISO 8601 with absent parts omitted.
     */
    @Test
    fun testToStringRendersIso8601() {

        assertEquals("2023", XmpDate.parse("2023").toString())
        assertEquals("2023-05", XmpDate.parse("2023-05").toString())
        assertEquals("2023-05-12", XmpDate.parse("2023-05-12").toString())
        assertEquals("2023-05-12T18:04:00", XmpDate.parse("2023-05-12T18:04").toString())
        assertEquals(
            "2023-05-12T18:04:07-05:00",
            XmpDate.parse("2023-05-12T18:04:07-05:00").toString()
        )
        assertEquals(
            "2023-05-12T18:04:07Z",
            XmpDate.parse("2023-05-12T18:04:07+00:00").toString()
        )
        assertEquals(
            "2023-05-12T18:04:07.5+05:30",
            XmpDate.parse("2023-05-12T18:04:07.5+0530").toString()
        )
    }

    /**
     * Parsing the rendered form of a parsed value delivers the same value again.
     */
    @Test
    fun testToStringRoundTrip() {

        val values = listOf(
            "2023-05-12T18:04:07-05:00",
            "2023-05-12T18:04:07.25Z",
            "2023-05-12",
            "2023",
            "2023-05-12T00:00:00+02:00"
        )

        for (value in values) {

            val parsed = XmpDate.parse(value)

            assertEquals(
                expected = parsed,
                actual = XmpDate.parse(requireNotNull(parsed).toString()),
                message = "value: $value"
            )
        }
    }
}
