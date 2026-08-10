package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.ParseOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests the alias handling of the XMP data model normalization.
 */
class XMPNormalizerTest {

    /**
     * A parsed alias property is moved to its base property when the base is missing.
     */
    @Test
    fun testAliasMovedToBaseProperty() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Titel",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )

        /* The alias name resolves to the base, so no xmp: schema may remain. */
        val paths = mutableListOf<String>()

        val iterator = xmpMeta.iterator()

        while (iterator.hasNext())
            paths.add(iterator.next().getPath())

        assertFalse(paths.any { it.contains("xmp:") })
    }

    /**
     * A parsed alias property is removed when the base property exists.
     */
    @Test
    fun testAliasRemovedWhenBaseExists() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Base</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Alias</rdf:li>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Base",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )

        /* The alias name resolves to the base, so no xmp: schema may remain. */
        val paths = mutableListOf<String>()

        val iterator = xmpMeta.iterator()

        while (iterator.hasNext())
            paths.add(iterator.next().getPath())

        assertFalse(paths.any { it.contains("xmp:") })
    }

    /**
     * Strict aliasing rejects a mismatch between alias and base values.
     */
    @Test
    fun testStrictAliasingMismatchThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Base</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Alias</rdf:li>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp, ParseOptions().setStrictAliasing(true))
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * Strict aliasing accepts matching alias and base subtrees.
     */
    @Test
    fun testStrictAliasingWithMatchingValues() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Same</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Same</rdf:li>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp, ParseOptions().setStrictAliasing(true))

        assertEquals(
            expected = "Same",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )
    }

    /**
     * Strict aliasing rejects a mismatch between inner node options.
     */
    @Test
    fun testStrictAliasingInnerOptionsMismatchThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">u</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default" rdf:resource="u"/>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp, ParseOptions().setStrictAliasing(true))
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * An alias to an alt-text array item is transplanted with an x-default qualifier.
     */
    @Test
    fun testAliasToAltTextItemGetsXDefaultQualifier() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <photoshop:Caption>Eine Bildunterschrift</photoshop:Caption>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Eine Bildunterschrift",
            actual = xmpMeta.getLocalizedText(
                XMPConst.NS_DC,
                "description",
                null,
                XMPConst.X_DEFAULT
            )!!.getValue()
        )
    }

    /**
     * An alias to an existing x-default base item is removed.
     */
    @Test
    fun testAliasToExistingXDefaultBaseRemoved() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Base</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <photoshop:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Alias</rdf:li>
                  </rdf:Alt>
                </photoshop:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Base",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )

        /* The alias name resolves to the base, so no photoshop schema may remain. */
        val paths = mutableListOf<String>()

        val iterator = xmpMeta.iterator()

        while (iterator.hasNext())
            paths.add(iterator.next().getPath())

        assertFalse(paths.any { it.contains("photoshop:") })
    }

    /**
     * Strict aliasing rejects an array item alias whose value differs from the
     * existing x-default item.
     */
    @Test
    fun testStrictAliasingArrayItemMismatchThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Base</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <photoshop:Title>Alias</photoshop:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp, ParseOptions().setStrictAliasing(true))
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * An alias to an array item is transplanted when the base array exists
     * without the x-default item.
     */
    @Test
    fun testAliasTransplantedWhenBaseArrayExistsWithoutXDefault() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="de">Deutsch</rdf:li>
                  </rdf:Alt>
                </dc:title>
                <photoshop:Title>Alias</photoshop:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Alias",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )
        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
    }

    /**
     * An alias to the x-default item with an existing language qualifier is rejected.
     */
    @Test
    fun testAliasToXDefaultWithLanguageQualifierThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
                <photoshop:Caption xml:lang="de">Bildunterschrift</photoshop:Caption>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(testXmp)
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }

    /**
     * The schema left empty by an alias move is removed.
     */
    @Test
    fun testEmptySchemaRemovedAfterAliasMove() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:Title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </xmp:Title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val paths = mutableListOf<String>()

        val iterator = xmpMeta.iterator()

        while (iterator.hasNext())
            paths.add(iterator.next().getPath())

        assertEquals(
            expected = listOf("", "dc:title", "dc:title[1]", "dc:title[1]/xml:lang"),
            actual = paths
        )
    }

    /**
     * Implicit nodes created before a path error are removed again.
     */
    @Test
    fun testImplicitNodesCleanedUpOnPathError() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            /* Indexing on a non-array fails after the implicit node was created. */
            xmpMeta.setProperty(XMPConst.NS_DC, "title[1]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))
    }

    /**
     * A setter through an alias to an alt-text array creates the base array
     * with an x-default item.
     */
    @Test
    fun testSetPropertyThroughAltTextAlias() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_PDF, "Title", "Der Titel")

        assertEquals(
            expected = "Der Titel",
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)!!.getValue()
        )
    }

    /**
     * A setter through an ordered array alias addresses the first item.
     */
    @Test
    fun testSetPropertyThroughArrayAlias() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "Author", "Autorin")

        assertEquals(
            expected = "Autorin",
            actual = xmpMeta.getArrayItem(XMPConst.NS_DC, "creator", 1)!!.getValue()
        )
    }

    /**
     * A setter through a simple alias writes the base property.
     */
    @Test
    fun testSetPropertyThroughSimpleAlias() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "Keywords", "fox")

        assertEquals("fox", xmpMeta.getPropertyString(XMPConst.NS_DC, "subject"))
    }
}
