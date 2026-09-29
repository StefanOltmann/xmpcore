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

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XMPMeta
import de.stefan_oltmann.xmp.XMPSchemaRegistry
import de.stefan_oltmann.xmp.internal.XMPNormalizer.normalize
import de.stefan_oltmann.xmp.options.ParseOptions
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction
import nl.adaptivity.xmlutil.dom2.attributes
import nl.adaptivity.xmlutil.dom2.childNodes
import nl.adaptivity.xmlutil.dom2.length
import nl.adaptivity.xmlutil.dom2.localName
import nl.adaptivity.xmlutil.dom2.namespaceURI
import nl.adaptivity.xmlutil.dom2.nodeType
import nl.adaptivity.xmlutil.dom2.prefix

/**
 * This class replaces the `ExpatAdapter.cpp` and does the XML-parsing and fixes the prefix.
 * After the parsing several normalisations are applied to the XMPTree.
 */
internal object XMPMetaParser {

    private val XMP_RDF = Any()

    /**
     * Maximum nesting depth of the XML containers that are searched for the RDF root. It
     * mirrors the DOM-level cap of DomParser, so neither guard of the recursive walks is
     * dead: real-world documents stay far below this, while hostile input can nest
     * arbitrarily deep and would exhaust the stack through the recursive search.
     */
    private const val MAX_SEARCH_DEPTH = 256

    /**
     * Parses the input source into an XMP metadata object, including
     * de-aliasing and normalisation.
     *
     * @param input   the XMP string
     * @param options the parse options
     * @return Returns the resulting XMP metadata object
     */
    fun parse(
        input: String,
        options: ParseOptions?
    ): XMPMeta {

        /*
         * An empty input is a parameter error like the ParameterAsserts of the Adobe
         * original; non-empty input that is not XML fails in DomParser below.
         */
        if (input.isEmpty())
            throw XMPException("Parameter must not be null or empty", XMPErrorConst.BADPARAM)

        val actualOptions = options ?: ParseOptions()

        /*
         * Control characters are repaired like the two-pass parse of the Adobe original:
         * literal invalid characters and numeric character references that resolve to one
         * become spaces, so they can never reach the data model.
         */
        val actualInput = if (actualOptions.getFixControlChars())
            Utils.replaceControlCharReferencesWithSpace(
                Utils.replaceControlCharsWithSpace(input)
            )
        else
            input

        val document = DomParser.parseDocumentFromString(actualInput)

        val xmpMetaRequired = actualOptions.getRequireXMPMeta()

        val result = findRootNode(document, xmpMetaRequired, arrayOfNulls(3), 0)

        if (result == null || result[1] !== XMP_RDF)
            throw XMPException("XMP RDF was not found.", XMPErrorConst.BADXMP)

        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        val rdfRoot = result[0] as Node

        preRegisterNamespaces(rdfRoot)

        val xmp = XMPRDFParser.parse(rdfRoot, actualOptions)

        xmp.setPacketHeader(result[2] as? String)

        /* Check if the XMP object shall be normalized */
        if (!actualOptions.getOmitNormalization())
            return normalize(xmp, actualOptions)

        return xmp
    }

    /**
     * Find the XML node that is the root of the XMP data tree. Generally this
     * will be an outer node, but it could be anywhere if a general XML document
     * is parsed (e.g. SVG). The XML parser counted all rdf:RDF and
     * pxmp:XMP_Packet nodes, and kept a pointer to the last one. If there is
     * more than one possible root use PickBestRoot to choose among them.
     *
     * If there is a root node, try to extract the version of the previous XMP
     * toolkit.
     *
     * Pick the first x:xmpmeta among multiple root candidates. If there aren't
     * any, pick the first bare rdf:RDF if that is allowed. The returned root is
     * the rdf:RDF child if an x:xmpmeta element was chosen. The search is
     * breadth first, so a higher level candiate is chosen over a lower level
     * one that was textually earlier in the serialized XML.
     *
     * @param root            the root of the xml document
     * @param xmpMetaRequired flag if the xmpmeta-tag is still required, might be set
     * initially to `true`, if the parse option "REQUIRE_XMP_META" is set
     * @param result          The result array that is filled during the recursive process.
     * @param depth           current XML nesting depth, used to bound the recursion.
     * @return Returns an array that contains the result or `null`.
     * The array contains:
     *
     *  * [0] - the rdf:RDF-node
     *  * [1] - an object that is either XMP_RDF or XMP_PLAIN (the latter is decrecated)
     *  * [2] - the body text of the xpacket-instruction.
     */
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private fun findRootNode(
        root: Node,
        xmpMetaRequired: Boolean,
        result: Array<Any?>,
        depth: Int
    ): Array<Any?>? {

        /*
         * Reject hostile documents that nest deeper than any legitimate XMP can, before the
         * recursion can exhaust the stack with a StackOverflowError that would escape the
         * XMPException contract.
         */
        if (depth > MAX_SEARCH_DEPTH)
            throw XMPException(
                "Maximum nesting depth of $MAX_SEARCH_DEPTH exceeded",
                XMPErrorConst.BADXMP
            )

        /*
         * Look among this parent's content for x:xapmeta or x:xmpmeta.
         * The recursion for x:xmpmeta is broader than the strictly
         * defined choice, but gives us smaller code.
         */

        @Suppress("LoopWithTooManyJumpStatements")
        for (index in 0 until root.childNodes.length) {

            val child = root.childNodes.item(index)

            requireNotNull(child)

            when {

                child.nodeType == NodeConsts.PROCESSING_INSTRUCTION_NODE &&
                    XMPConst.XMP_PI == (child as ProcessingInstruction).getTarget() -> {

                    /* Store the processing instructions content */
                    result[2] = child.getData()
                }

                /* Ignore comments */
                child.nodeType == NodeConsts.COMMENT_NODE -> continue

                /*
                 * Only elements can contain or be the RDF root. Any other node type here
                 * (e.g. a document type declaration that slipped through) is ignored instead
                 * of crashing on an invalid cast.
                 */
                child.nodeType == NodeConsts.ELEMENT_NODE -> {

                    val childElement = child as Element

                    val rootNS = childElement.namespaceURI

                    val rootLocal = childElement.localName

                    if (
                        (XMPConst.TAG_XMPMETA == rootLocal || XMPConst.TAG_XAPMETA == rootLocal) &&
                        XMPConst.NS_X == rootNS
                    ) {

                        /* By not passing the RequireXMPMeta-option, the rdf-Node will be valid */
                        return findRootNode(child, false, result, depth + 1)
                    }

                    if (!xmpMetaRequired && "RDF" == rootLocal && XMPConst.NS_RDF == rootNS) {

                        result[0] = child
                        result[1] = XMP_RDF

                        return result
                    }

                    /* Continue searching */
                    val newResult = findRootNode(child, xmpMetaRequired, result, depth + 1)

                    return newResult ?: continue
                }
            }
        }

        /* Return NULL if no appropriate node has been found. */
        return null
    }

    /**
     * Registers the namespaces the RDF subtree uses before the tree is walked. The walk
     * itself registers namespaces lazily in DOM attribute order, which is not specified and
     * differs between platforms, so namespaces whose suggested prefixes collide would
     * receive different generated prefixes on each platform. Sorting the pre-registration by
     * URI makes the assignment deterministic on every platform.
     *
     * @param rdfRoot The rdf:RDF-node the metadata is parsed from.
     */
    private fun preRegisterNamespaces(rdfRoot: Node) {

        val namespaceToPrefix = mutableMapOf<String, String>()

        collectUsedNamespaces(rdfRoot, 0, namespaceToPrefix)

        var newCount = 0

        for (namespaceURI in namespaceToPrefix.keys.sorted()) {

            if (XMPSchemaRegistry.getNamespacePrefix(namespaceURI) != null)
                continue

            if (newCount >= XMPMeta.MAX_AUTO_REGISTERED_NAMESPACES_PER_DOCUMENT)
                throw XMPException(
                    "Cannot register '$namespaceURI': a single document may not introduce more than "
                        + "${XMPMeta.MAX_AUTO_REGISTERED_NAMESPACES_PER_DOCUMENT} namespaces",
                    XMPErrorConst.BADSCHEMA
                )

            newCount++

            XMPSchemaRegistry.registerNamespace(namespaceURI, requireNotNull(namespaceToPrefix[namespaceURI]))
        }
    }

    /**
     * Collects one suggested prefix per namespace URI used in the subtree. If a namespace is
     * declared under several prefixes, the lexicographically smallest one wins, so the
     * choice does not depend on the DOM attribute order.
     */
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private fun collectUsedNamespaces(
        node: Node,
        depth: Int,
        into: MutableMap<String, String>
    ) {

        if (depth > MAX_SEARCH_DEPTH)
            throw XMPException(
                "Maximum nesting depth of $MAX_SEARCH_DEPTH exceeded",
                XMPErrorConst.BADXMP
            )

        if (node.nodeType == NodeConsts.ELEMENT_NODE) {

            val element = node as Element

            rememberNamespace(element.namespaceURI, element.prefix, into)

            val attributes = element.attributes

            for (index in 0 until attributes.getLength()) {

                val attribute = attributes.item(index) ?: continue

                rememberNamespace(attribute.namespaceURI, attribute.prefix, into)
            }
        }

        for (index in 0 until node.childNodes.length) {

            val child = requireNotNull(node.childNodes.item(index))

            if (child.nodeType == NodeConsts.ELEMENT_NODE)
                collectUsedNamespaces(child, depth + 1, into)
        }
    }

    private fun rememberNamespace(
        namespaceURI: String?,
        prefix: String?,
        into: MutableMap<String, String>
    ) {

        /* The declaration namespace itself and prefix-less namespaces need no registration. */
        if (namespaceURI.isNullOrEmpty() || prefix.isNullOrEmpty() || prefix == "xmlns")
            return

        val existing = into[namespaceURI]

        if (existing == null || prefix < existing)
            into[namespaceURI] = prefix
    }
}
