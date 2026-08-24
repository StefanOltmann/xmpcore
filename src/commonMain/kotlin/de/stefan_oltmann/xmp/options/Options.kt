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

import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.internal.Utils
import de.stefan_oltmann.xmp.internal.XMPErrorConst

/**
 * The base class for a collection of 32 flag bits. Individual flags are defined as enum value bit
 * masks. Inheriting classes add convenience accessor methods.
 */
public abstract class Options {

    /**
     * the internal int containing all options.
     */
    private var valueBits = 0

    protected constructor()

    /**
     * Constructor with the options bit mask.
     *
     * @param options the options bit mask
     *
     */
    protected constructor(options: Int) {
        assertOptionsValid(options)
        setOptions(options)
    }

    protected abstract fun getValidOptions(): Int

    /**
     * @param optionBit the binary bit or bits that are requested
     * @return Returns if *all* of the requested bits are set or not.
     */
    protected fun getOption(optionBit: Int): Boolean =
        valueBits and optionBit != 0

    /**
     * @param optionBits the binary bit or bits that shall be set to the given value
     * @param value      the boolean value to set
     */
    public fun setOption(optionBits: Int, value: Boolean) {
        this.valueBits = if (value)
            this.valueBits or optionBits
        else
            this.valueBits and optionBits.inv()
    }

    /**
     * Is friendly to access it during the tests.
     *
     * @return Returns the options.
     */
    public fun getOptions(): Int = valueBits

    /**
     * Replaces the complete option bit mask after validating it.
     *
     * @param options The options to set.
     * @throws XMPException If undefined or inconsistent option bits are set.
     */
    public fun setOptions(options: Int) {

        assertOptionsValid(options)

        this.valueBits = options
    }

    override fun equals(other: Any?): Boolean =
        getOptions() == (other as? Options)?.getOptions()

    override fun hashCode(): Int = getOptions()

    /**
     * Creates a human readable string from the set options. *Note:* This method is quite
     * expensive and should only be used within tests or as debug output.
     *
     * @return Returns a string listing all options that are set to `true` by their name,
     * like "option1 | option4".
     */
    public fun getOptionsString(): String {

        if (valueBits == 0)
            return "<none>"

        val sb = StringBuilder()
        var remainingBits = valueBits

        while (remainingBits != 0) {

            /* Isolate the rightmost set bit and clear it from the remainder. */
            val singleBit = remainingBits xor (remainingBits and (remainingBits - 1))
            sb.append(getOptionName(singleBit))

            remainingBits = remainingBits and (remainingBits - 1)

            if (remainingBits != 0)
                sb.append(" | ")
        }

        return sb.toString()
    }

    /**
     * @return Returns the options as hex bitmask.
     */
    override fun toString(): String =
        "0x" + valueBits.toString(Utils.HEX_RADIX)

    /**
     * To be implemeted by inheritants.
     *
     * @param option a single, valid option bit.
     * @return Returns a human-readable name for an option bit.
     */
    protected abstract fun defineOptionName(option: Int): String?

    /**
     * Looks up the name of a single option bit via [Options.defineOptionName],
     * with a fallback for bits without a defined name.
     *
     * @param option a single option bit
     * @return Returns the option name or `"<option name not defined>"`.
     */
    private fun getOptionName(option: Int): String =
        defineOptionName(option) ?: "<option name not defined>"

    /**
     * The inheriting option class can do additional checks on the options.
     * *Note:* For performance reasons this method is only called
     * when setting bitmasks directly.
     * When get- and set-methods are used, this method must be called manually,
     * normally only when the Options-object has been created from a client
     * (it has to be made public therefore).
     *
     * @param options the bitmask to check.
     *
     */
    protected open fun assertConsistency(options: Int): Unit = Unit

    /**
     * Checks options before they are set.
     * First it is checked if only defined options are used,
     * second the additional [Options.assertConsistency]-method is called.
     */
    private fun assertOptionsValid(options: Int) {

        val invalidOptions = options and getValidOptions().inv()

        if (invalidOptions != 0)
            throw XMPException(
                "The option bit(s) 0x" + invalidOptions.toString(Utils.HEX_RADIX) + " + are invalid!",
                XMPErrorConst.BADOPTIONS
            )

        assertConsistency(options)
    }
}
