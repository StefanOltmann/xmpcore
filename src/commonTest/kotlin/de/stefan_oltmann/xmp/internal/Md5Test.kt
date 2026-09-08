package de.stefan_oltmann.xmp.internal

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests [Md5] against the test vectors of RFC 1321, which is the source of the GUID of
 * Adobe extended XMP packets.
 */
class Md5Test {

    /**
     * The seven RFC 1321 test vectors produce their documented digests.
     */
    @Test
    fun testRfc1321Vectors() {

        val vectors = listOf(
            "" to "d41d8cd98f00b204e9800998ecf8427e",
            "a" to "0cc175b9c0f1b6a831c399e269772661",
            "abc" to "900150983cd24fb0d6963f7d28e17f72",
            "message digest" to "f96b697d7cb7938d525a2f31aaf161d0",
            "abcdefghijklmnopqrstuvwxyz" to "c3fcd3d76192e4007dfb496cca67e13b",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                to "d174ab98d277d9f5a5611c2c9f419d9f",
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                to "57edf4a22be3c955ac49da2e2107b67a"
        )

        for ((input, expected) in vectors) {

            val digest = Md5.toHexString(Md5.digest(input.encodeToByteArray()))

            assertEquals(
                expected = expected,
                actual = digest,
                message = "input: $input"
            )
        }
    }
}
