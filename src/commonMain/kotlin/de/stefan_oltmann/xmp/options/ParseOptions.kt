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
package de.stefan_oltmann.xmp.options

/**
 * Options for [XMPMetaFactory.parse].
 */
public class ParseOptions : Options() {

    init {

        /* The Adobe default enables the repair options. */
        setOption(FIX_CONTROL_CHARS, true)
    }

    /**
     * @return Returns the requireXMPMeta.
     */
    public fun getRequireXMPMeta(): Boolean =
        getOption(REQUIRE_XMP_META)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setRequireXMPMeta(value: Boolean): ParseOptions {
        setOption(REQUIRE_XMP_META, value)
        return this
    }

    /**
     * @return Returns whether control characters are replaced with spaces while parsing.
     */
    public fun getFixControlChars(): Boolean =
        getOption(FIX_CONTROL_CHARS)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setFixControlChars(value: Boolean): ParseOptions {
        setOption(FIX_CONTROL_CHARS, value)
        return this
    }

    /**
     * @return Returns the strictAliasing.
     */
    public fun getStrictAliasing(): Boolean =
        getOption(STRICT_ALIASING)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setStrictAliasing(value: Boolean): ParseOptions {
        setOption(STRICT_ALIASING, value)
        return this
    }

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setOmitNormalization(value: Boolean): ParseOptions {
        setOption(OMIT_NORMALIZATION, value)
        return this
    }

    /**
     * @return Returns the option "omit normalization".
     */
    public fun getOmitNormalization(): Boolean =
        getOption(OMIT_NORMALIZATION)

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            REQUIRE_XMP_META -> "REQUIRE_XMP_META"
            FIX_CONTROL_CHARS -> "FIX_CONTROL_CHARS"
            STRICT_ALIASING -> "STRICT_ALIASING"
            OMIT_NORMALIZATION -> "OMIT_NORMALIZATION"
            else -> null
        }
    }

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        REQUIRE_XMP_META or FIX_CONTROL_CHARS or STRICT_ALIASING or OMIT_NORMALIZATION

    /**
     * Require a surrounding "x:xmpmeta" element in the xml-document.
     */
    internal companion object {

        const val REQUIRE_XMP_META = 0x0001

        /**
         * The default-on Adobe option to replace ASCII control characters with spaces
         * while parsing.
         */
        const val FIX_CONTROL_CHARS = 0x0008

        /**
         * Do not reconcile alias differences, throw an exception instead.
         */
        const val STRICT_ALIASING = 0x0004

        /**
         * Do not carry run the XMPNormalizer on a packet, leave it as it is.
         */
        const val OMIT_NORMALIZATION = 0x0020
    }
}
