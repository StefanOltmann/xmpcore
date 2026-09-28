package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [XMPUtils.appendProperties] against the documented behavior of the Adobe XMP
 * Core: missing properties are appended, existing values are kept or replaced, structs
 * and arrays are merged, and internal properties need an explicit opt-in.
 */
class XMPUtilsAppendPropertiesTest {

    /**
     * A property missing in the destination is copied over.
     */
    @Test
    fun testAppendCopiesMissingProperty() {

        val source = parse("source", "<dc:title>Haltestelle</dc:title>")

        val destinationXml = """
            <dc:subject>
              <rdf:Bag>
                <rdf:li>fox</rdf:li>
              </rdf:Bag>
            </dc:subject>
        """.trimIndent()

        val destination = parse("destination", destinationXml)

        XMPUtils.appendProperties(source, destination, false, false, false)

        assertEquals("Haltestelle", readXDefault(destination, "title"))

        assertTrue(destination.doesPropertyExist(XMPConst.NS_DC, "subject"))
    }

    /**
     * An existing simple value is kept when replaceOldValues is off and replaced when on.
     */
    @Test
    fun testAppendExistingSimpleValue() {

        val source = parse("source", "<dc:title>From Source</dc:title>")
        val destination = parse("destination", "<dc:title>From Destination</dc:title>")

        XMPUtils.appendProperties(source, destination, false, false, false)

        assertEquals("From Destination", readXDefault(destination, "title"))

        XMPUtils.appendProperties(source, destination, false, true, false)

        assertEquals("From Source", readXDefault(destination, "title"))
    }

    /**
     * Other arrays merge by item values without duplicates.
     */
    @Test
    fun testAppendMergesArrayItems() {

        val sourceXml = """
            <dc:subject>
              <rdf:Bag>
                <rdf:li>swiper</rdf:li>
              </rdf:Bag>
            </dc:subject>
        """.trimIndent()

        val source = parse("source", sourceXml)

        val destinationXml = """
            <dc:subject>
              <rdf:Bag>
                <rdf:li>fox</rdf:li>
              </rdf:Bag>
            </dc:subject>
        """.trimIndent()

        val destination = parse("destination", destinationXml)

        XMPUtils.appendProperties(source, destination, false, false, false)

        assertEquals(setOf("fox", "swiper"), destination.getKeywords())
    }

    /**
     * AltText arrays merge by language, and an existing x-default is not replaced.
     */
    @Test
    fun testAppendMergesAltTextByLanguage() {

        val sourceXml = """
            <dc:description>
              <rdf:Alt>
                <rdf:li xml:lang="x-default">Source Default</rdf:li>
                <rdf:li xml:lang="de">Quelle</rdf:li>
              </rdf:Alt>
            </dc:description>
        """.trimIndent()

        val source = parse("source", sourceXml)

        val destinationXml = """
            <dc:description>
              <rdf:Alt>
                <rdf:li xml:lang="x-default">Destination Default</rdf:li>
              </rdf:Alt>
            </dc:description>
        """.trimIndent()

        val destination = parse("destination", destinationXml)

        XMPUtils.appendProperties(source, destination, false, false, false)

        assertEquals("Destination Default", readXDefault(destination, "description"))

        assertEquals(
            "Quelle",
            destination.getLocalizedText(XMPConst.NS_DC, "description", "", "de")?.getValue()
        )
    }

    /**
     * A source value that is empty deletes the destination property when
     * deleteEmptyValues is set.
     */
    @Test
    fun testAppendDeleteEmptyValues() {

        val source = parse("source", "<dc:title></dc:title>")
        val destination = parse("destination", "<dc:title>From Destination</dc:title>")

        XMPUtils.appendProperties(source, destination, false, false, true)

        assertFalse(destination.doesPropertyExist(XMPConst.NS_DC, "title"))
    }

    /**
     * Internal properties are only appended on explicit opt-in.
     */
    @Test
    fun testAppendSkipsInternalProperties() {

        val source = parse("source", "<xmp:ModifyDate>2020-05-01T08:15:30</xmp:ModifyDate>")
        val destination = parse("destination", "<dc:title>Haltestelle</dc:title>")

        XMPUtils.appendProperties(source, destination, false, false, false)

        assertFalse(destination.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))

        XMPUtils.appendProperties(source, destination, true, false, false)

        assertTrue(destination.doesPropertyExist(XMPConst.NS_XMP, "ModifyDate"))
    }

    private fun parse(about: String, properties: String): XMPMeta {

        val rdf = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="urn:$about"
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                $properties
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        return XMPMetaFactory.parseFromString(rdf)
    }

    /**
     * Reads the x-default value of a Dublin Core alt-text property.
     */
    private fun readXDefault(xmpMeta: XMPMeta, propName: String): String? {

        val property = xmpMeta.getLocalizedText(XMPConst.NS_DC, propName, "", XMPConst.X_DEFAULT)

        return property?.getValue()
    }
}
