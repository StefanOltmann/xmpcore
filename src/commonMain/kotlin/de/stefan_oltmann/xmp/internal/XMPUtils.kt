/*
 * =================================================================================================
 * ADOBE SYSTEMS INCORPORATED
 * Copyright 2006 Adobe Systems Incorporated
 * All Rights Reserved
 *
 * NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the terms
 * of the Adobe license agreement accompanying it.
 * =================================================================================================
 */
package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Utility methods for XMP. I included only those that are different from the
 * Java default conversion utilities.
 */
internal object XMPUtils {

    /**
     * The XML whitespace characters that Adobe's base64 decoder ignores.
     */
    private val BASE64_WHITESPACE = charArrayOf(' ', '\t', '\r', '\n')

    /**
     * Convert from string to Boolean.
     *
     * @param value The string representation of the Boolean.
     * @return The appropriate boolean value for the string.
     *         The checked values for `true` and `false` are:
     *  * [XMPConst.TRUE_STRING] and [XMPConst.FALSE_STRING]
     *  * &quot;t&quot; and &quot;f&quot;
     *  * &quot;on&quot; and &quot;off&quot;
     *  * &quot;yes&quot; and &quot;no&quot;
     *  * &quot;value != 0&quot; and &quot;value == 0&quot;
     */
    @kotlin.jvm.JvmStatic
    fun convertToBoolean(value: String?): Boolean {

        if (value.isNullOrEmpty())
            throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

        val valueLowercase = value.lowercase()

        /* First try interpretation as Integer (anything not 0 is true) */
        val asInteger = valueLowercase.toIntOrNull()

        if (asInteger != null)
            return asInteger != 0

        return when (valueLowercase) {

            "true", "t", "on", "yes" -> true

            "false", "f", "off", "no" -> false

            /*
             * Like Adobe, an unrecognized string must surface as an error instead of
             * silently reporting a confident false for corrupted property values.
             */
            else -> throw XMPException("Invalid Boolean string", XMPErrorConst.BADVALUE)
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToInteger(rawValue: String?): Int {
        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return if (rawValue.startsWith("0x"))
                rawValue.substring(2).toInt(Utils.HEX_RADIX)
            else
                rawValue.toInt()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid integer string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToLong(rawValue: String?): Long {

        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return if (rawValue.startsWith("0x"))
                rawValue.substring(2).toLong(Utils.HEX_RADIX)
            else
                rawValue.toLong()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid long string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @kotlin.jvm.JvmStatic
    fun convertToDouble(rawValue: String?): Double {

        try {

            if (rawValue.isNullOrEmpty())
                throw XMPException(XMPErrorConst.EMPTY_CONVERT_STRING_TEXT, XMPErrorConst.BADVALUE)

            return rawValue.toDouble()

        } catch (ex: NumberFormatException) {
            throw XMPException("Invalid double string", XMPErrorConst.BADVALUE, ex)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @kotlin.jvm.JvmStatic
    fun encodeBase64(buffer: ByteArray): String =
        Base64.encode(buffer)

    @OptIn(ExperimentalEncodingApi::class)
    @kotlin.jvm.JvmStatic
    fun decodeBase64(base64String: String): ByteArray {

        /*
         * Real-world packets wrap long base64 values across lines. XML whitespace inside the
         * data must be ignored like Adobe's decoder does; every other character outside the
         * alphabet stays an error so corrupted values are not silently accepted.
         */
        val compactBase64 = buildString {
            for (char in base64String)
                if (char !in BASE64_WHITESPACE)
                    append(char)
        }

        try {

            return Base64.decode(compactBase64.encodeToByteArray())

        } catch (ex: Throwable) {
            throw XMPException("Invalid base64 string", XMPErrorConst.BADVALUE, ex)
        }
    }
}
