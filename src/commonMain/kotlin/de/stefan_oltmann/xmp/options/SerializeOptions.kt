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
 * Options for [de.stefan_oltmann.xmp.XMPMetaFactory.serializeToString].
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
    private var padding: Int = 0

    /**
     * The string to be used as line terminator in the serialized output.
     */
    private var newline: String = "\n"

    /**
     * The string to be used for each level of indentation in the serialized output.
     */
    private var indent: String = "  "

    /**
     * The number of levels of indentation for the outermost XML element in the serialized
     * output.
     */
    private var baseIndent: Int = 0

    /**
     * Default constructor.
     */
    public constructor()

    /**
     * Constructor using inital options.
     *
     * @param options the inital options
     *
     */
    internal constructor(options: Int) : super(options)

    /**
     * @return Returns whether the `<?xpacket ...?>` packet wrapper shall be omitted.
     */
    public fun getOmitPacketWrapper(): Boolean =
        getOption(OMIT_PACKET_WRAPPER)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setOmitPacketWrapper(value: Boolean): SerializeOptions {
        setOption(OMIT_PACKET_WRAPPER, value)
        return this
    }

    /**
     * @return Returns whether the `x:xmpmeta` element shall be omitted.
     */
    public fun getOmitXmpMetaElement(): Boolean =
        getOption(OMIT_XMPMETA_ELEMENT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setOmitXmpMetaElement(value: Boolean): SerializeOptions {
        setOption(OMIT_XMPMETA_ELEMENT, value)
        return this
    }

    /**
     * @return Returns whether the packet is marked as read-only.
     */
    public fun getReadOnlyPacket(): Boolean =
        getOption(READONLY_PACKET)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setReadOnlyPacket(value: Boolean): SerializeOptions {
        setOption(READONLY_PACKET, value)
        return this
    }

    /**
     * @return Returns whether the compact form of RDF is requested.
     */
    public fun getUseCompactFormat(): Boolean =
        getOption(USE_COMPACT_FORMAT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setUseCompactFormat(value: Boolean): SerializeOptions {
        setOption(USE_COMPACT_FORMAT, value)
        return this
    }

    /**
     * @return Returns whether the canonical form of RDF is requested.
     */
    public fun getUseCanonicalFormat(): Boolean =
        getOption(USE_CANONICAL_FORMAT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setUseCanonicalFormat(value: Boolean): SerializeOptions {
        setOption(USE_CANONICAL_FORMAT, value)
        return this
    }

    /**
     * @return Returns whether the data model is sorted before serializing.
     */
    public fun getSort(): Boolean =
        getOption(SORT)

    /**
     * @param value the value to set
     * @return Returns the instance to call more set-methods.
     */
    public fun setSort(value: Boolean): SerializeOptions {
        setOption(SORT, value)
        return this
    }

    /**
     * @return Returns the padding.
     */
    public fun getPadding(): Int =
        padding

    /**
     * @param value The amount of padding. Must not be negative.
     * @return Returns the instance to call more set-methods.
     */
    public fun setPadding(value: Int): SerializeOptions {

        require(value >= 0) { "Padding must not be negative: $value" }

        padding = value
        return this
    }

    /**
     * @return Returns the newline.
     */
    public fun getNewline(): String =
        newline

    /**
     * @param value The line terminator, for example `\n` or `\r\n`.
     * @return Returns the instance to call more set-methods.
     */
    public fun setNewline(value: String): SerializeOptions {
        newline = value
        return this
    }

    /**
     * @return Returns the indent.
     */
    public fun getIndent(): String =
        indent

    /**
     * @param value The string used for each level of indentation, for example two spaces.
     * @return Returns the instance to call more set-methods.
     */
    public fun setIndent(value: String): SerializeOptions {
        indent = value
        return this
    }

    /**
     * @return Returns the baseIndent.
     */
    public fun getBaseIndent(): Int =
        baseIndent

    /**
     * @param value The number of indentation levels for the outermost XML element.
     * @return Returns the instance to call more set-methods.
     */
    public fun setBaseIndent(value: Int): SerializeOptions {

        require(value >= 0) { "Base indent must not be negative: $value" }

        baseIndent = value
        return this
    }

    /**
     * @return Returns clone of this SerializeOptions-object with the same options set.
     */
    public fun clone(): SerializeOptions =
        SerializeOptions(getOptions())
            .setPadding(padding)
            .setNewline(newline)
            .setIndent(indent)
            .setBaseIndent(baseIndent)

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
