package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The expected values pin the JVM's Double.toString rendering, because
 * written metadata must be byte-identical on all platforms.
 * On JS and WASM this test fails for plain Double.toString whenever the
 * platform deviates (whole numbers without ".0", exponent thresholds).
 */
class DoubleInvariantStringTest {

    @Test
    fun testWholeNumbersKeepFractionDigit() {

        assertEquals("0.0", 0.0.toInvariantString())

        assertEquals("-0.0", (-0.0).toInvariantString())

        assertEquals("1.0", 1.0.toInvariantString())

        assertEquals("300.0", 300.0.toInvariantString())

        assertEquals("100.0", 100.0.toInvariantString())

        assertEquals("-300.0", (-300.0).toInvariantString())
    }

    @Test
    fun testFractionsRenderAsIs() {

        assertEquals("0.1", 0.1.toInvariantString())

        assertEquals("0.25", 0.25.toInvariantString())

        assertEquals("7.1", 7.1.toInvariantString())

        assertEquals("54.678955", 54.678955.toInvariantString())

        assertEquals("0.3333333333333333", (1.0 / 3.0).toInvariantString())
    }

    @Test
    fun testPlainNotationRangeMatchesJvm() {

        /* 1.0E-3 itself is still plain. */
        assertEquals("0.001", 0.001.toInvariantString())

        /* Smaller values switch to exponent notation. */
        assertEquals("1.0E-4", 0.0001.toInvariantString())

        assertEquals("9.9999E-5", 0.000099999.toInvariantString())

        assertEquals("1.25E-5", 1.25E-5.toInvariantString())

        assertEquals("5.0E-6", 5.0E-6.toInvariantString())
    }

    @Test
    fun testExponentNotationRangeMatchesJvm() {

        /* 9999999.0 itself is still plain. */
        assertEquals("9999999.0", 9_999_999.0.toInvariantString())

        /* 1.0E7 and above switch to exponent notation. */
        assertEquals("1.0E7", 1.0E7.toInvariantString())

        assertEquals("1.23456789E8", 1.23456789E8.toInvariantString())
    }

    @Test
    fun testSpecialValues() {

        assertEquals("NaN", Double.NaN.toInvariantString())

        assertEquals("Infinity", Double.POSITIVE_INFINITY.toInvariantString())

        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.toInvariantString())
    }
}
