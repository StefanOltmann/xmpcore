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
import de.stefan_oltmann.xmp.internal.XMPErrorConst

/**
 * Options for [de.stefan_oltmann.xmp.XMPMetaFactory.serializeToString].
 *
 * Instances are immutable: the fluent setters return a modified copy and leave the receiver
 * unchanged, so one shared instance can be reused from concurrent writers without locking.
 * The inherited raw mutators [setOption] and [setOptions] throw, because calling them on a
 * shared instance would break that guarantee; use the copy-returning setters instead.
 */
public class SerializeOptions : Options {

    /**
     * The amount of padding to be added if a writeable XML packet is created.
     *
     * The whitespace is written between the packet content and the `<?xpacket end?>`
     * processing instruction so that metadata can later be updated in place without
     * rewriting the rest of the container file.
     *
     * Note: Adobe's SDK pads 2048 characters by default. This port emits no padding unless
     * requested, because sidecar files (the primary use case here) are rewritten as a whole
     * and existing consumers rely on the current output.
     */
    private val padding: Int

    /**
     * The string to be used as line terminator in the serialized output.
     */
    private val newline: String

    /**
     * The string to be used for each level of indentation in the serialized output.
     */
    private val indent: String

    /**
     * The number of levels of indentation for the outermost XML element in the serialized
     * output.
     */
    private val baseIndent: Int

    /**
     * Default constructor.
     */
    public constructor() : super() {
        padding = 0
        newline = "\n"
        indent = "  "
        baseIndent = 0
    }

    /**
     * Constructor using inital options.
     *
     * @param options the inital options
     *
     */
    internal constructor(options: Int) : super(options) {
        padding = 0
        newline = "\n"
        indent = "  "
        baseIndent = 0
    }

    /**
     * Copies all values of an existing instance.
     */
    private constructor(
        options: Int,
        padding: Int,
        newline: String,
        indent: String,
        baseIndent: Int
    ) : super(options) {
        this.padding = padding
        this.newline = newline
        this.indent = indent
        this.baseIndent = baseIndent
    }

    /**
     * Attention: The inherited raw mutator would break the immutability of a shared
     * instance and therefore always fails; use the copy-returning setters instead.
     */
    public override fun setOption(optionBits: Int, value: Boolean): Unit =
        throwImmutableMutator()

    /**
     * Attention: The inherited raw mutator would break the immutability of a shared
     * instance and therefore always fails; use the copy-returning setters instead.
     */
    public override fun setOptions(options: Int): Unit =
        throwImmutableMutator()

    /**
     * Builds the exception for the unusable inherited raw mutators.
     */
    private fun throwImmutableMutator(): Nothing =
        throw XMPException(
            "SerializeOptions is immutable, use the copy-returning set methods instead",
            XMPErrorConst.BADOPTIONS
        )

    /**
     * @return Returns whether the `<?xpacket ...?>` packet wrapper shall be omitted.
     */
    public fun getOmitPacketWrapper(): Boolean =
        getOption(OMIT_PACKET_WRAPPER)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setOmitPacketWrapper(value: Boolean): SerializeOptions =
        withOption(OMIT_PACKET_WRAPPER, value)

    /**
     * @return Returns whether the `x:xmpmeta` element shall be omitted.
     */
    public fun getOmitXmpMetaElement(): Boolean =
        getOption(OMIT_XMPMETA_ELEMENT)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setOmitXmpMetaElement(value: Boolean): SerializeOptions =
        withOption(OMIT_XMPMETA_ELEMENT, value)

    /**
     * @return Returns whether the packet is marked as read-only.
     */
    public fun getReadOnlyPacket(): Boolean =
        getOption(READONLY_PACKET)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setReadOnlyPacket(value: Boolean): SerializeOptions =
        withOption(READONLY_PACKET, value)

    /**
     * @return Returns whether the compact form of RDF is requested.
     */
    public fun getUseCompactFormat(): Boolean =
        getOption(USE_COMPACT_FORMAT)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setUseCompactFormat(value: Boolean): SerializeOptions =
        withOption(USE_COMPACT_FORMAT, value)

    /**
     * @return Returns whether the canonical form of RDF is requested.
     */
    public fun getUseCanonicalFormat(): Boolean =
        getOption(USE_CANONICAL_FORMAT)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setUseCanonicalFormat(value: Boolean): SerializeOptions =
        withOption(USE_CANONICAL_FORMAT, value)

    /**
     * @return Returns whether the data model is sorted before serializing.
     */
    public fun getSort(): Boolean =
        getOption(SORT)

    /**
     * @param value the value to set
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setSort(value: Boolean): SerializeOptions =
        withOption(SORT, value)

    /**
     * @return Returns the padding.
     */
    public fun getPadding(): Int =
        padding

    /**
     * @param value The amount of padding. Must not be negative.
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setPadding(value: Int): SerializeOptions {

        require(value >= 0) { "Padding must not be negative: $value" }

        return copyOf(padding = value)
    }

    /**
     * @return Returns the newline.
     */
    public fun getNewline(): String =
        newline

    /**
     * @param value The line terminator, for example `\n` or `\r\n`.
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setNewline(value: String): SerializeOptions =
        copyOf(newline = value)

    /**
     * @return Returns the indent.
     */
    public fun getIndent(): String =
        indent

    /**
     * @param value The string used for each level of indentation, for example two spaces.
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setIndent(value: String): SerializeOptions =
        copyOf(indent = value)

    /**
     * @return Returns the baseIndent.
     */
    public fun getBaseIndent(): Int =
        baseIndent

    /**
     * @param value The number of indentation levels for the outermost XML element.
     * @return Returns a modified copy to call more set-methods on.
     */
    public fun setBaseIndent(value: Int): SerializeOptions {

        require(value >= 0) { "Base indent must not be negative: $value" }

        return copyOf(baseIndent = value)
    }

    /**
     * @return Returns copy of this SerializeOptions-object with the same options set.
     */
    public fun clone(): SerializeOptions =
        copyOf()

    private fun copyOf(
        options: Int = this.getOptions(),
        padding: Int = this.padding,
        newline: String = this.newline,
        indent: String = this.indent,
        baseIndent: Int = this.baseIndent
    ): SerializeOptions =
        SerializeOptions(options, padding, newline, indent, baseIndent)

    private fun withOption(optionBits: Int, value: Boolean): SerializeOptions {

        val adjustedBits = if (value)
            getOptions() or optionBits
        else
            getOptions() and optionBits.inv()

        return copyOf(options = adjustedBits)
    }

    /**
     * @see Options.defineOptionName
     */
    override fun defineOptionName(option: Int): String? {
        return when (option) {
            OMIT_PACKET_WRAPPER -> "OMIT_PACKET_WRAPPER"
            READONLY_PACKET -> "READONLY_PACKET"
            USE_COMPACT_FORMAT -> "USE_COMPACT_FORMAT"
            OMIT_XMPMETA_ELEMENT -> "OMIT_XMPMETA_ELEMENT"
            SORT -> "NORMALIZED"
            else -> null
        }
    }

    /**
     * @see Options.getValidOptions
     */
    override fun getValidOptions(): Int =
        OMIT_PACKET_WRAPPER or READONLY_PACKET or
            USE_COMPACT_FORMAT or USE_CANONICAL_FORMAT or
            OMIT_XMPMETA_ELEMENT or SORT

    internal companion object {

        /**
         * Omit the XML packet wrapper.
         */
        const val OMIT_PACKET_WRAPPER = 0x0010

        /**
         * Mark packet as read-only. Default is a writeable packet.
         */
        const val READONLY_PACKET = 0x0020

        /**
         * Use a compact form of RDF.
         * The compact form is the default serialization format (this flag is technically ignored).
         * To serialize to the canonical form, set the flag USE_CANONICAL_FORMAT.
         * If both flags &quot;compact&quot; and &quot;canonical&quot; are set, canonical is used.
         */
        const val USE_COMPACT_FORMAT = 0x0040

        /**
         * Use the canonical form of RDF if set. By default the compact form is used
         */
        const val USE_CANONICAL_FORMAT = 0x0080

        /**
         * Omit the &lt;x:xmpmeta&gt;-tag.
         */
        const val OMIT_XMPMETA_ELEMENT = 0x1000

        /**
         * Sort the struct properties and qualifier before serializing.
         */
        const val SORT = 0x2000
    }
}
