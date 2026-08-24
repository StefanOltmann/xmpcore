package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the value conversion helpers of [XMPUtils].
 */
class XMPUtilsTest {

    /**
     * The canonical true values and integers unequal zero convert to true.
     */
    @Test
    fun testConvertToBooleanTrue() {

        assertTrue(XMPUtils.convertToBoolean("True"))
        assertTrue(XMPUtils.convertToBoolean("true"))
        assertTrue(XMPUtils.convertToBoolean("t"))
        assertTrue(XMPUtils.convertToBoolean("T"))
        assertTrue(XMPUtils.convertToBoolean("on"))
        assertTrue(XMPUtils.convertToBoolean("yes"))
        assertTrue(XMPUtils.convertToBoolean("1"))
        assertTrue(XMPUtils.convertToBoolean("-3"))
    }

    /**
     * The canonical false values and zero convert to false.
     */
    @Test
    fun testConvertToBooleanFalse() {

        assertFalse(XMPUtils.convertToBoolean("False"))
        assertFalse(XMPUtils.convertToBoolean("false"))
        assertFalse(XMPUtils.convertToBoolean("f"))
        assertFalse(XMPUtils.convertToBoolean("off"))
        assertFalse(XMPUtils.convertToBoolean("no"))
        assertFalse(XMPUtils.convertToBoolean("0"))
    }

    /**
     * Unrecognized values like garbage or hex integers are rejected like Adobe does,
     * empty values too.
     */
    @Test
    fun testConvertToBooleanInvalid() {

        assertFailsWith<XMPException> {
            XMPUtils.convertToBoolean("xyz")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPUtils.convertToBoolean("0x0")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }

        val ex = assertFailsWith<XMPException> {
            XMPUtils.convertToBoolean("")
        }
        assertEquals(XMPErrorConst.BADVALUE, ex.errorCode)
    }

    /**
     * Decimal and hex integers convert correctly.
     */
    @Test
    fun testConvertToInteger() {

        assertEquals(42, XMPUtils.convertToInteger("42"))
        assertEquals(-7, XMPUtils.convertToInteger("-7"))
        assertEquals(31, XMPUtils.convertToInteger("0x1F"))
        assertEquals(0, XMPUtils.convertToInteger("0x0"))
    }

    /**
     * Empty and invalid integer strings are rejected.
     */
    @Test
    fun testConvertToIntegerInvalid() {

        assertFailsWith<XMPException> {
            XMPUtils.convertToInteger("")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPUtils.convertToInteger("abc")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * Long values beyond the int range convert correctly.
     */
    @Test
    fun testConvertToLong() {

        assertEquals(12_345_678_901_234L, XMPUtils.convertToLong("12345678901234"))
        assertEquals(255L, XMPUtils.convertToLong("0xFF"))

        assertFailsWith<XMPException> {
            XMPUtils.convertToLong("abc")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPUtils.convertToLong("")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * Double values convert correctly, invalid ones are rejected.
     */
    @Test
    fun testConvertToDouble() {

        assertEquals(3.5, XMPUtils.convertToDouble("3.5"))
        assertEquals(-0.25, XMPUtils.convertToDouble("-0.25"))
        assertEquals(1000.0, XMPUtils.convertToDouble("1e3"))

        assertFailsWith<XMPException> {
            XMPUtils.convertToDouble("abc")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPUtils.convertToDouble("")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * Base64 encoding and decoding are inverse operations.
     */
    @Test
    fun testEncodeAndDecodeBase64() {

        val bytes = byteArrayOf(1, 2, 3, 4)

        val encoded = XMPUtils.encodeBase64(bytes)

        assertEquals("AQIDBA==", encoded)

        assertContentEquals(bytes, XMPUtils.decodeBase64(encoded))
    }

    /**
     * Invalid base64 input is rejected.
     */
    @Test
    fun testDecodeBase64Invalid() {

        assertFailsWith<XMPException> {
            XMPUtils.decodeBase64("!!!")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * XML whitespace from wrapped lines is ignored like Adobe's decoder does, while other
     * invalid characters stay rejected.
     */
    @Test
    fun testDecodeBase64IgnoresWhitespace() {

        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6)

        val wrapped = buildString {
            append("AQID")
            append('\r')
            append('\n')
            append("BAUG")
            append('\t')
            append("  ")
            append('\n')
        }

        assertContentEquals(bytes, XMPUtils.decodeBase64(wrapped))

        /* Whitespace-only input decodes to empty data instead of failing. */
        assertContentEquals(byteArrayOf(), XMPUtils.decodeBase64(" \r\n\t"))

        /* Other junk between the alphabet characters remains an error. */
        assertFailsWith<XMPException> {
            XMPUtils.decodeBase64("AQ!ID")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }
}
