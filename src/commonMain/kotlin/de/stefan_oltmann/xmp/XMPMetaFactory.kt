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
package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.internal.XMPMetaParser
import de.stefan_oltmann.xmp.internal.XMPRDFWriter
import de.stefan_oltmann.xmp.options.ParseOptions
import de.stefan_oltmann.xmp.options.SerializeOptions

/**
 * Creates `XMPMeta`-instances and serializes them to a String. This is the entry point for
 * parsing XMP packets.
 */
public object XMPMetaFactory {

    @kotlin.jvm.JvmStatic
    public val schemaRegistry: XMPSchemaRegistry = XMPSchemaRegistry

    @kotlin.jvm.JvmStatic
    public val versionInfo: XMPVersionInfo = XMPVersionInfo

    /**
     * Creates an empty `XMPMeta`-object.
     */
    @kotlin.jvm.JvmStatic
    public fun create(): XMPMeta = XMPMeta()

    /**
     * Creates an `XMPMeta`-object from a string.
     *
     * @param packet A String containing an XMP-file.
     * @param options Options controlling the parsing.
     * @return Returns the `XMPMeta`-object created from the input.
     * @throws XMPException If the file is not well-formed XML or if the parsing fails.
     */
    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun parseFromString(
        packet: String,
        options: ParseOptions? = null
    ): XMPMeta {

        try {

            return XMPMetaParser.parse(packet, options)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Parsing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }

    /**
     * Serializes an `XMPMeta`-object as RDF into a string.
     * Note: Encoding is ignored when serializing to a string.
     *
     * @param xmp A metadata object.
     * @param options Options to control the serialization (see [SerializeOptions]).
     * @return Returns a String containing the serialized RDF.
     * @throws XMPException On serialization errors.
     */
    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    @Throws(XMPException::class)
    public fun serializeToString(
        xmp: XMPMeta,
        options: SerializeOptions? = null
    ): String {

        try {

            val actualOptions = options ?: SerializeOptions()

            /* Sort the internal data model on demand */
            if (actualOptions.getSort())
                xmp.sort()

            return XMPRDFWriter(actualOptions).serialize(xmp)

        } catch (ex: XMPException) {

            throw ex

        } catch (ex: Exception) {

            /*
             * Ensure that only XMPException is thrown from this method.
             * Wrap all other exceptions accordingly.
             */

            throw XMPException("Serializing error.", XMPErrorConst.UNKNOWN, ex)
        }
    }
}
