package de.stefan_oltmann.xmp.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the byte order mark handling of [XmpPacketDecoder].
 */
class XmpPacketDecoderTest {

    /**
     * UTF-8 with a byte order mark is decoded without the mark.
     */
    @Test
    fun testDecodesUtf8WithBom() {

        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "Titel".encodeToByteArray()

        assertEquals("Titel", XmpPacketDecoder.decode(bytes))
    }

    /**
     * Without a byte order mark the content is treated as strict UTF-8.
     */
    @Test
    fun testDecodesUtf8WithoutBom() {

        val bytes = "Straße".encodeToByteArray()

        assertEquals("Straße", XmpPacketDecoder.decode(bytes))
    }

    /**
     * Malformed UTF-8 fails instead of being silently replaced with U+FFFD.
     */
    @Test
    fun testRejectsMalformedUtf8() {

        /* A lone continuation byte is never valid. */
        val bytes = byteArrayOf(0x41.toByte(), 0x82.toByte(), 0x42.toByte())

        assertFailsWith<IllegalArgumentException> {
            XmpPacketDecoder.decode(bytes)
        }
    }

    /**
     * UTF-16LE with a byte order mark is decoded.
     */
    @Test
    fun testDecodesUtf16Le() {

        val text = "Straße"

        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + encodeUtf16(text, littleEndian = true)

        assertEquals(text, XmpPacketDecoder.decode(bytes))
    }

    /**
     * UTF-16BE with a byte order mark is decoded.
     */
    @Test
    fun testDecodesUtf16Be() {

        val text = "Straße"

        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + encodeUtf16(text, littleEndian = false)

        assertEquals(text, XmpPacketDecoder.decode(bytes))
    }

    /**
     * Supplementary characters encoded as surrogate pairs survive the decoding.
     */
    @Test
    fun testDecodesUtf16SurrogatePairs() {

        val text = "a\ud83d\ude00b"

        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + encodeUtf16(text, littleEndian = true)

        assertEquals(text, XmpPacketDecoder.decode(bytes))
    }

    /**
     * An odd trailing byte cannot form a code unit and is rejected like other corrupted
     * content instead of being silently dropped from the packet.
     */
    @Test
    fun testRejectsOddTrailingByteInUtf16() {

        val bytes = byteArrayOf(
            0xFF.toByte(), 0xFE.toByte(),
            0x41.toByte(), 0x00.toByte(),
            0x42.toByte()
        )

        assertFailsWith<IllegalArgumentException> {
            XmpPacketDecoder.decode(bytes)
        }
    }

    /**
     * A high surrogate without a following low surrogate is rejected instead of producing a
     * string that fails downstream far away from the root cause.
     */
    @Test
    fun testRejectsUnpairedHighSurrogateInUtf16() {

        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            encodeUtf16("a\ud83db", littleEndian = true)

        assertFailsWith<IllegalArgumentException> {
            XmpPacketDecoder.decode(bytes)
        }
    }

    /**
     * A high surrogate at the very end of the content has no pair and is rejected.
     */
    @Test
    fun testRejectsTrailingHighSurrogateInUtf16() {

        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            encodeUtf16("\ud83d", littleEndian = true)

        assertFailsWith<IllegalArgumentException> {
            XmpPacketDecoder.decode(bytes)
        }
    }

    /**
     * A low surrogate without a preceding high surrogate is rejected.
     */
    @Test
    fun testRejectsUnpairedLowSurrogateInUtf16() {

        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            encodeUtf16("\ude00", littleEndian = true)

        assertFailsWith<IllegalArgumentException> {
            XmpPacketDecoder.decode(bytes)
        }
    }

    /**
     * Encodes text as raw UTF-16 code units in the given byte order.
     */
    private fun encodeUtf16(text: String, littleEndian: Boolean): ByteArray {

        val bytes = ByteArray(text.length * 2)

        for ((charIndex, char) in text.withIndex()) {

            val unit = char.code

            if (littleEndian) {
                bytes[charIndex * 2] = (unit and 0xFF).toByte()
                bytes[charIndex * 2 + 1] = (unit shr 8).toByte()
            } else {
                bytes[charIndex * 2] = (unit shr 8).toByte()
                bytes[charIndex * 2 + 1] = (unit and 0xFF).toByte()
            }
        }

        return bytes
    }
}
