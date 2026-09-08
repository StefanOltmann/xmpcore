package de.stefan_oltmann.xmp.internal

/**
 * Pure Kotlin implementation of the MD5 message digest (RFC 1321).
 *
 * The GUID of an Adobe extended XMP packet is the MD5 digest of its data, and multiplatform
 * code cannot use java.security, so the algorithm is implemented here. Only the digest
 * operation is provided, which is all the extended XMP scheme requires.
 */
@Suppress("MagicNumber")
internal object Md5 {

    private const val HEX_DIGITS = "0123456789abcdef"

    /*
     * The per-round shift amounts and the sine table from RFC 1321, hardcoded because
     * computing them from sin() is not guaranteed to round identically on every platform.
     */
    private val SHIFT_AMOUNTS =
        intArrayOf(
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
        )

    private val SINE_CONSTANTS =
        intArrayOf(
            0xd76aa478.toInt(), 0xe8c7b756.toInt(), 0x242070db, 0xc1bdceee.toInt(),
            0xf57c0faf.toInt(), 0x4787c62a, 0xa8304613.toInt(), 0xfd469501.toInt(),
            0x698098d8, 0x8b44f7af.toInt(), 0xffff5bb1.toInt(), 0x895cd7be.toInt(),
            0x6b901122, 0xfd987193.toInt(), 0xa679438e.toInt(), 0x49b40821,
            0xf61e2562.toInt(), 0xc040b340.toInt(), 0x265e5a51, 0xe9b6c7aa.toInt(),
            0xd62f105d.toInt(), 0x02441453, 0xd8a1e681.toInt(), 0xe7d3fbc8.toInt(),
            0x21e1cde6, 0xc33707d6.toInt(), 0xf4d50d87.toInt(), 0x455a14ed,
            0xa9e3e905.toInt(), 0xfcefa3f8.toInt(), 0x676f02d9, 0x8d2a4c8a.toInt(),
            0xfffa3942.toInt(), 0x8771f681.toInt(), 0x6d9d6122, 0xfde5380c.toInt(),
            0xa4beea44.toInt(), 0x4bdecfa9, 0xf6bb4b60.toInt(), 0xbebfbc70.toInt(),
            0x289b7ec6, 0xeaa127fa.toInt(), 0xd4ef3085.toInt(), 0x04881d05,
            0xd9d4d039.toInt(), 0xe6db99e5.toInt(), 0x1fa27cf8, 0xc4ac5665.toInt(),
            0xf4292244.toInt(), 0x432aff97, 0xab9423a7.toInt(), 0xfc93a039.toInt(),
            0x655b59c3, 0x8f0ccc92.toInt(), 0xffeff47d.toInt(), 0x85845dd1.toInt(),
            0x6fa87e4f, 0xfe2ce6e0.toInt(), 0xa3014314.toInt(), 0x4e0811a1,
            0xf7537e82.toInt(), 0xbd3af235.toInt(), 0x2ad7d2bb, 0xeb86d391.toInt()
        )

    /**
     * Computes the 16-byte MD5 digest of the input.
     */
    fun digest(input: ByteArray): ByteArray {

        var a = 0x67452301
        var b = -0x10325477
        var c = -0x67452302
        var d = 0x10325476

        val padded = pad(input)

        for (offset in padded.indices step 64) {

            val chunkResult = processChunk(padded, offset, a, b, c, d)

            a += chunkResult[0]
            b += chunkResult[1]
            c += chunkResult[2]
            d += chunkResult[3]
        }

        val result = ByteArray(16)

        writeIntLE(a, result, 0)
        writeIntLE(b, result, 4)
        writeIntLE(c, result, 8)
        writeIntLE(d, result, 12)

        return result
    }

    /**
     * Renders the digest as lowercase hexadecimal.
     */
    fun toHexString(digest: ByteArray): String = buildString(digest.size * 2) {

        for (byte in digest) {

            append(HEX_DIGITS[(byte.toInt() shr 4) and 0x0F])
            append(HEX_DIGITS[byte.toInt() and 0x0F])
        }
    }

    /**
     * Appends the 0x80 terminator, the zero padding and the little-endian bit length, so the
     * length becomes a multiple of 64 bytes.
     */
    private fun pad(input: ByteArray): ByteArray {

        val paddedLength = ((input.size + 8) / 64 + 1) * 64

        val padded = ByteArray(paddedLength)

        input.copyInto(padded)

        padded[input.size] = 0x80.toByte()

        val bitLength = input.size.toLong() * 8

        for (index in 0 until 8)
            padded[paddedLength - 8 + index] = (bitLength ushr (8 * index)).toByte()

        return padded
    }

    /**
     * Processes one 64-byte chunk of the padded input starting from the running digest state
     * and returns the four state words to add to it.
     */
    private fun processChunk(
        chunk: ByteArray,
        offset: Int,
        stateA: Int,
        stateB: Int,
        stateC: Int,
        stateD: Int
    ): IntArray {

        val message = IntArray(16)

        for (index in 0 until 16)
            message[index] = readIntLE(chunk, offset + index * 4)

        var a = stateA
        var b = stateB
        var c = stateC
        var d = stateD

        for (index in 0 until 64) {

            val (f, g) = when (index / 16) {

                0 -> ((b and c) or (b.inv() and d)) to index

                1 -> ((d and b) or (d.inv() and c)) to (5 * index + 1) % 16

                2 -> (b xor c xor d) to (3 * index + 5) % 16

                else -> (c xor (b or d.inv())) to (7 * index) % 16
            }

            val newB = b + (f + a + SINE_CONSTANTS[index] + message[g]).rotateLeft(SHIFT_AMOUNTS[index])

            a = d
            d = c
            c = b
            b = newB
        }

        return intArrayOf(a, b, c, d)
    }

    private fun readIntLE(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeIntLE(value: Int, target: ByteArray, offset: Int) {

        target[offset] = value.toByte()
        target[offset + 1] = (value shr 8).toByte()
        target[offset + 2] = (value shr 16).toByte()
        target[offset + 3] = (value shr 24).toByte()
    }
}
