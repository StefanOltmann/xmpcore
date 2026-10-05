package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The rendering must keep every value the parser can produce:
 * offset-less midnight keeps its time part, and partially specified or
 * out-of-range calendar parts never render as malformed literals.
 */
class XmpDateRenderingTest {

    /**
     * Midnight without an offset is a complete, valid time - dropping
     * "T00:00:00" on a read-modify-write would silently change the
     * written value to a date-only literal.
     */
    @Test
    fun testOffsetLessMidnightKeepsItsTime() {

        val parsed = XmpDate.parse("2023-05-12T00:00:00")

        assertEquals("2023-05-12T00:00:00", parsed.toString())

        /* The round trip is stable: the re-parsed render is identical. */
        assertEquals(
            "2023-05-12T00:00:00",
            XmpDate.parse(parsed.toString()).toString()
        )
    }

    /**
     * A day without a month cannot be rendered as a valid literal - the
     * day is omitted together with the month instead of emitting
     * "2023--05"-shaped garbage.
     */
    @Test
    fun testDayWithoutMonthIsOmitted() {

        val date = XmpDate(
            year = 2023,
            month = 0,
            day = 5,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0,
            utcOffsetMinutes = null
        )

        assertEquals("2023", date.toString())
    }

    /**
     * Negative years render with the sign in front of the zero-padded
     * digits, not inside them.
     */
    @Test
    fun testNegativeYearRendersSigned() {

        val date = XmpDate(
            year = -44,
            month = 3,
            day = 15,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0,
            utcOffsetMinutes = null
        )

        assertEquals("-0044-03-15", date.toString())
    }
}
