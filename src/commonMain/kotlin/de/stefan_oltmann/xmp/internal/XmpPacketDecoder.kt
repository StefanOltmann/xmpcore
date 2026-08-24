package de.stefan_oltmann.xmp.internal

/**
 * Decodes raw XMP packet bytes into text.
 *
 * The XMP specification allows packets to be UTF-8 or UTF-16, marked by a byte order mark.
 * UTF-8 is decoded strictly, so malformed sequences fail instead of being silently replaced
 * with U+FFFD, which would corrupt metadata.
 */
internal object XmpPacketDecoder {

    /**
     * Mask to read a byte as an unsigned value.
     */
    private const val BYTE_MASK = 0xFF

    /**
     * Shift to move a byte into the high position of a UTF-16 code unit.
     */
    private const val HIGH_BYTE_SHIFT = 8

    /**
     * Length of the UTF-8 byte order mark.
     */
    private const val UTF8_BOM_LENGTH = 3

    /**
     * Length of the UTF-16 byte order mark.
     */
    private const val UTF16_BOM_LENGTH = 2

    private val utf8Bom = intArrayOf(0xEF, 0xBB, 0xBF)

    private val utf16LeBom = intArrayOf(0xFF, 0xFE)

    private val utf16BeBom = intArrayOf(0xFE, 0xFF)

    /**
     * Decodes the given packet bytes according to their byte order mark.
     *
     * Without a mark the bytes are treated as strict UTF-8, which reliably rejects UTF-32
     * packets instead of corrupting them.
     *
     * @param bytes the raw packet bytes.
     * @return Returns the decoded packet text.
     * @throws IllegalArgumentException if the content is not well-formed UTF-8 or UTF-16.
     */
    fun decode(bytes: ByteArray): String =
        when {
            startsWith(bytes, utf8Bom) -> decodeUtf8(bytes, UTF8_BOM_LENGTH)
            startsWith(bytes, utf16LeBom) -> decodeUtf16(bytes, littleEndian = true)
            startsWith(bytes, utf16BeBom) -> decodeUtf16(bytes, littleEndian = false)
            else -> decodeUtf8(bytes, 0)
        }

    /**
     * Strictly decodes UTF-8 starting after the byte order mark, if any.
     */
    private fun decodeUtf8(bytes: ByteArray, offset: Int): String {

        try {

            return bytes.decodeToString(startIndex = offset, throwOnInvalidSequence = true)

        } catch (ex: Exception) {

            /*
             * Platforms differ in what they report for malformed sequences: the JVM throws
             * CharacterCodingException, others IllegalArgumentException. Normalize so that
             * callers only have to handle one exception type.
             */
            throw IllegalArgumentException("Malformed UTF-8 content", ex)
        }
    }

    /**
     * Decodes UTF-16 code units after the byte order mark.
     *
     * Like the strict UTF-8 path, corrupted content fails instead of being silently accepted:
     * an odd trailing byte cannot form a code unit and unpaired surrogates would corrupt the
     * packet in downstream text processing far away from the root cause.
     */
    private fun decodeUtf16(bytes: ByteArray, littleEndian: Boolean): String {

        val builder = StringBuilder((bytes.size - UTF16_BOM_LENGTH) / 2)

        var index = UTF16_BOM_LENGTH

        while (index + 1 < bytes.size) {

            val unit = readCodeUnit(bytes, littleEndian, index)

            index += 2

            when {
                unit.isHighSurrogate() -> {

                    if (index + 1 >= bytes.size)
                        throw IllegalArgumentException("Unpaired high surrogate at end of content")

                    val lowUnit = readCodeUnit(bytes, littleEndian, index)

                    if (!lowUnit.isLowSurrogate())
                        throw IllegalArgumentException("Unpaired high surrogate in UTF-16 content")

                    builder.append(unit)

                    builder.append(lowUnit)

                    index += 2
                }

                unit.isLowSurrogate() ->
                    throw IllegalArgumentException("Unpaired low surrogate in UTF-16 content")

                else -> builder.append(unit)
            }
        }

        if (index != bytes.size)
            throw IllegalArgumentException("Odd trailing byte in UTF-16 content")

        return builder.toString()
    }

    /**
     * Reads one UTF-16 code unit at [index] honoring the byte order.
     */
    private fun readCodeUnit(bytes: ByteArray, littleEndian: Boolean, index: Int): Char {

        val first = bytes[index].toInt() and BYTE_MASK

        val second = bytes[index + 1].toInt() and BYTE_MASK

        return if (littleEndian)
            (first or (second shl HIGH_BYTE_SHIFT)).toChar()
        else
            ((first shl HIGH_BYTE_SHIFT) or second).toChar()
    }

    /**
     * Checks whether the bytes start with the given byte order mark.
     */
    private fun startsWith(bytes: ByteArray, mark: IntArray): Boolean {

        if (bytes.size < mark.size)
            return false

        for (index in mark.indices)
            if ((bytes[index].toInt() and BYTE_MASK) != mark[index])
                return false

        return true
    }
}
