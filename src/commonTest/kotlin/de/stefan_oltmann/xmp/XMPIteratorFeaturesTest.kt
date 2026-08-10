package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.IteratorOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the remaining [XMPIterator] features: subtree skipping, property filters,
 * leaf names and error handling.
 */
class XMPIteratorFeaturesTest {

    private val testXmp = """
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
          <rdf:Description rdf:about=""
              xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:subject>
              <rdf:Bag>
                <rdf:li>fox</rdf:li>
                <rdf:li>swiper</rdf:li>
              </rdf:Bag>
            </dc:subject>
            <dc:title>
              <rdf:Alt>
                <rdf:li xml:lang="x-default">Titel</rdf:li>
              </rdf:Alt>
            </dc:title>
          </rdf:Description>
        </rdf:RDF>
    """.trimIndent()

    /**
     * Skipping the siblings after the current node ends the iteration.
     */
    @Test
    fun testSkipSiblings() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator()

        /* Consume the schema node. */
        iterator.next()

        /* dc:subject is the first child; skip it and all following siblings. */
        iterator.next()
        iterator.skipSiblings()

        assertEquals(
            expected = emptyList(),
            actual = collectPaths(iterator)
        )
    }

    /**
     * Skipping the subtree below a property continues the iteration with the next sibling.
     */
    @Test
    fun testSkipSubtree() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator()

        /* Consume the schema node and dc:subject. */
        iterator.next()
        iterator.next()

        iterator.skipSubtree()

        assertEquals(
            expected = listOf(
                "dc:title",
                "dc:title[1]",
                "dc:title[1]/xml:lang"
            ),
            actual = collectPaths(iterator)
        )
    }

    /**
     * Skipping the subtree below the schema node ends the iteration.
     */
    @Test
    fun testSkipSubtreeOnSchemaNode() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator()

        iterator.next()

        iterator.skipSubtree()

        assertFalse(iterator.hasNext())
    }

    /**
     * Skipping the subtree below a leaf node continues the iteration with its next sibling.
     */
    @Test
    fun testSkipSubtreeOnLeafNode() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "subject", null)

        /* Consume dc:subject and its first array item. */
        iterator.next()
        iterator.next()

        iterator.skipSubtree()

        assertEquals(
            expected = listOf("dc:subject[2]"),
            actual = collectPaths(iterator)
        )
    }

    /**
     * Skipping the subtree of the root property of a property path iteration ends the iteration.
     */
    @Test
    fun testSkipSubtreeEndsIteration() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "title", null)

        iterator.next()

        iterator.skipSubtree()

        assertFalse(iterator.hasNext())
    }

    /**
     * The option `JUST_LEAFNAME` returns only the last path component. The namespace prefix
     * is only stripped from qualifiers, array items are reduced to their index.
     */
    @Test
    fun testJustLeafnameOption() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "",
                "dc:subject",
                "[1]",
                "[2]",
                "dc:title",
                "[1]",
                "xml:lang"
            ),
            actual = collectPaths(
                xmpMeta.iterator(IteratorOptions().setJustLeafname(true))
            )
        )
    }

    /**
     * The option `OMIT_QUALIFIERS` skips the qualifier nodes.
     */
    @Test
    fun testOmitQualifiersOption() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "",
                "dc:subject",
                "dc:subject[1]",
                "dc:subject[2]",
                "dc:title",
                "dc:title[1]"
            ),
            actual = collectPaths(
                xmpMeta.iterator(IteratorOptions().setOmitQualifiers(true))
            )
        )
    }

    /**
     * Iterating a subtree starting at a specific property path visits that property first.
     */
    @Test
    fun testPropertyPathIterator() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "dc:title",
                "dc:title[1]",
                "dc:title[1]/xml:lang"
            ),
            actual = collectPaths(
                xmpMeta.iterator(XMPConst.NS_DC, "title", null)
            )
        )
    }

    /**
     * A property path without a schema namespace is rejected.
     */
    @Test
    fun testPropertyPathWithoutSchemaThrows() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val ex = assertFailsWith<XMPException> {
            xmpMeta.iterator(null, "title", null)
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * Iterating a non-existent property path yields an empty iteration.
     */
    @Test
    fun testNonExistentPropertyPathYieldsNothing() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "doesNotExist", null)

        assertFalse(iterator.hasNext())
    }

    /**
     * `next()` throws when the iteration is exhausted.
     */
    @Test
    fun testNextOnExhaustedIteratorThrows() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "doesNotExist", null)

        assertFailsWith<NoSuchElementException> {
            iterator.next()
        }
    }

    /**
     * The option `JUST_CHILDREN` combined with `JUST_LEAFNODES` skips the composite
     * children, so nothing is left to report.
     */
    @Test
    fun testJustChildrenAndLeafnodesCombination() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = emptyList(),
            actual = collectPaths(
                xmpMeta.iterator(
                    XMPConst.NS_DC,
                    null,
                    IteratorOptions().setJustChildren(true).setJustLeafnodes(true)
                )
            )
        )
    }

    /**
     * The iterator over an empty XMP object reports nothing.
     */
    @Test
    fun testEmptyXmpIteration() {

        val xmpMeta = XMPMetaFactory.create()

        val iterator = xmpMeta.iterator()

        assertFalse(iterator.hasNext())
    }

    /**
     * The schema of a property with an unknown prefix falls back to the base schema.
     */
    @Test
    fun testNamespaceFallbackForUnprefixedLeaves() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "title[1]", null)

        assertTrue(iterator.hasNext())

        assertEquals(XMPConst.NS_DC, iterator.next().getNamespace())
    }

    /**
     * `next()` after the full iteration is exhausted throws.
     */
    @Test
    fun testNextAfterFullIterationThrows() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "title", null)

        while (iterator.hasNext())
            iterator.next()

        assertFailsWith<NoSuchElementException> {
            iterator.next()
        }
    }

    /**
     * The language of iterated properties is not reported.
     */
    @Test
    fun testLanguageIsNotReported() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "title", null)

        while (iterator.hasNext()) {
            val propertyInfo = iterator.next()
            assertEquals(null, propertyInfo.getLanguage())
        }
    }

    /**
     * Skipping the siblings also works with the `JUST_CHILDREN` iterator.
     */
    @Test
    fun testSkipSiblingsWithJustChildren() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(
            XMPConst.NS_DC,
            null,
            IteratorOptions().setJustChildren(true)
        )

        iterator.next()
        iterator.skipSiblings()

        assertFalse(iterator.hasNext())
    }

    private fun collectPaths(iterator: XMPIterator): List<String> {

        val paths = mutableListOf<String>()

        while (iterator.hasNext()) {
            val propertyInfo = iterator.next()
            paths.add(propertyInfo.getPath())
        }

        return paths
    }
}
