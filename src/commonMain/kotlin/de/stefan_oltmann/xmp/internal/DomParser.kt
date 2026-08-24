package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPException
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.writeCurrent
import nl.adaptivity.xmlutil.xmlStreaming

internal object DomParser {

    private const val RDF_RDF_END = "</rdf:RDF>"

    private const val SINGLE_SELF_CLOSING_RDF_TAG =
        """<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"/>"""

    /**
     * Marker of an XML DOCTYPE declaration. The keyword is case-sensitive per the XML
     * specification, so this exact spelling covers every well-formed declaration.
     */
    private const val DOCTYPE_MARKER = "<!DOCTYPE"

    @OptIn(XmlUtilInternal::class, ExperimentalXmlUtilApi::class)
    fun parseDocumentFromString(input: String): Document {

        /*
         * Checked on the raw input before anything else, so neither the initial parse nor the
         * corrupted-file recovery can process or salvage a DOCTYPE-carrying document by
         * slicing around the declaration.
         */
        rejectDoctype(input)

        return try {
            parseDocumentFromStringInternal(input)
        } catch (ex: Exception) {
            parseCorruptedDocument(input, ex)
        }
    }

    /**
     * Rejects documents containing a DOCTYPE declaration.
     *
     * XMP never uses DTDs. Untrusted input declaring internal or external entities must be
     * turned away before any parsing happens, so entities can never be expanded (billion
     * laughs) or resolved (XXE). Checking the raw input up front also keeps the corrupted-file
     * recovery below from salvaging a DOCTYPE document by slicing around the declaration.
     */
    private fun rejectDoctype(input: String) {

        if (input.contains(DOCTYPE_MARKER))
            throw XMPException(
                "DTD declarations are not supported in XMP.",
                XMPErrorConst.BADSTREAM
            )
    }

    /**
     * Parses a document that failed the full parse, tolerating junk around the RDF part.
     *
     * @param input the full document input
     * @param originalException the error of the full parse, thrown if no fallback works
     */
    private fun parseCorruptedDocument(input: String, originalException: Exception): Document {

        /*
         * Some corrupted files contain junk around the RDF part, e.g. NUL characters
         * or random text, which the XML parser rejects. First try parsing the prefix
         * up to the end of the RDF part and its enclosing element, which keeps the
         * xpacket processing instruction and all namespace declarations.
         */
        val firstLt = input.indexOf('<')
        val rdfEndPos = input.indexOf(RDF_RDF_END)

        if (firstLt >= 0 && rdfEndPos >= 0) {

            var cutEnd = rdfEndPos + RDF_RDF_END.length

            val nextCloseTag = input.indexOf("</", cutEnd)

            if (nextCloseTag >= 0) {

                val closeTagEnd = input.indexOf('>', nextCloseTag)

                if (closeTagEnd >= 0)
                    cutEnd = closeTagEnd + 1
            }

            val cleanedDocument = parseDocumentOrNull(input.substring(firstLt until cutEnd))

            if (cleanedDocument != null)
                return cleanedDocument

            /*
             * Files with junk before the RDF part are trimmed down to the RDF part.
             */
            val rdfStartPos = input.indexOf("<rdf:RDF")

            if (rdfStartPos >= 0) {

                val trimmedDocument = parseDocumentOrNull(
                    input.substring(rdfStartPos until rdfEndPos + RDF_RDF_END.length)
                )

                if (trimmedDocument != null)
                    return trimmedDocument
            }
        }

        /*
         * Without an end tag the document may contain a single self-closing RDF tag.
         */
        if (input.contains(SINGLE_SELF_CLOSING_RDF_TAG))
            return parseDocumentFromStringInternal(SINGLE_SELF_CLOSING_RDF_TAG)

        throw originalException
    }

    /**
     * Parses the input or returns null if the input cannot be parsed.
     */
    private fun parseDocumentOrNull(input: String): Document? =
        try {
            parseDocumentFromStringInternal(input)
        } catch (ignored: Exception) {
            null
        }

    @OptIn(ExperimentalXmlUtilApi::class)
    private fun parseDocumentFromStringInternal(input: String): Document {

        rejectDoctype(input)

        if (input.isBlank())
            throw XMPException("XMP is empty.", XMPErrorConst.BADXMP)

        try {

            val document = xmlStreaming.genericDomImplementation.createDocument(
                namespace = null,
                qualifiedName = null,
                documentType = null
            )

            val writer = xmlStreaming.newWriter(document)

            val reader = xmlStreaming.newReader(input)

            do {
                val event = reader.next()
                reader.writeCurrent(writer)
            } while (event != EventType.END_DOCUMENT)

            return document

        } catch (ex: Exception) {
            throw XMPException("Error reading the XML file: ${ex.message}", XMPErrorConst.BADSTREAM, ex)
        }
    }
}
