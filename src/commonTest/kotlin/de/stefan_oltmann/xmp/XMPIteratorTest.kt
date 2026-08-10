/*
 * Copyright 2026 Stefan Oltmann
 */
package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.IteratorOptions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for the `XMPIterator`.
 *
 * Previously the iterator crashed with a `NullPointerException` when iterating the whole tree,
 * a whole schema, or with the option `JUST_CHILDREN`, because schema nodes and composite nodes
 * have no path and no value. These tests guard the exact iteration output.
 */
class XMPIteratorTest {

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
     * Iterating the whole tree visits the schema node first, then the properties, array items
     * and qualifiers in document order. Schema and composite nodes have no path and no value.
     */
    @Test
    fun testWholeTreeIterator() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "",
                "dc:subject",
                "dc:subject[1]",
                "dc:subject[2]",
                "dc:title",
                "dc:title[1]",
                "dc:title[1]/xml:lang"
            ),
            actual = collectPaths(xmpMeta.iterator())
        )
    }

    /**
     * Iterating one schema without a property name visits the schema node and its whole subtree.
     */
    @Test
    fun testSchemaOnlyIterator() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "",
                "dc:subject",
                "dc:subject[1]",
                "dc:subject[2]",
                "dc:title",
                "dc:title[1]",
                "dc:title[1]/xml:lang"
            ),
            actual = collectPaths(xmpMeta.iterator(XMPConst.NS_DC, null, null))
        )
    }

    /**
     * The option `JUST_CHILDREN` reports the direct children of the start node without
     * descending into their subtrees. From the root these are the schema nodes, from a schema
     * node these are the top level properties.
     */
    @Test
    fun testJustChildrenIterator() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        /* From the root only the schema nodes are direct children. */
        val wholeTreeIterator = xmpMeta.iterator(IteratorOptions().setJustChildren(true))

        assertEquals(
            expected = listOf(""),
            actual = collectPaths(wholeTreeIterator)
        )

        /* From a schema the direct children are the top level properties. */
        val schemaIterator = xmpMeta.iterator(
            XMPConst.NS_DC,
            null,
            IteratorOptions().setJustChildren(true)
        )

        assertEquals(
            expected = listOf("dc:subject", "dc:title"),
            actual = collectPaths(schemaIterator)
        )
    }

    /**
     * The option `JUST_LEAFNODES` skips schema and composite nodes and reports only the leaves.
     */
    @Test
    fun testJustLeafnodesIterator() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = listOf(
                "dc:subject[1]",
                "dc:subject[2]",
                "dc:title[1]",
                "dc:title[1]/xml:lang"
            ),
            actual = collectPaths(xmpMeta.iterator(IteratorOptions().setJustLeafnodes(true)))
        )
    }

    /**
     * Schema and composite nodes have no value; array items without a namespace prefix resolve
     * their namespace from the schema, and the xml:lang qualifier reports the XML namespace.
     */
    @Test
    fun testValuesAndNamespaces() {

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator()

        val values = mutableListOf<String>()
        val namespaces = mutableListOf<String>()

        while (iterator.hasNext()) {
            val propertyInfo = iterator.next()
            values.add(propertyInfo.getValue())
            namespaces.add(propertyInfo.getNamespace())
        }

        assertEquals(
            expected = listOf("", "", "fox", "swiper", "", "Titel", "x-default"),
            actual = values
        )

        assertEquals(
            expected = List(6) { XMPConst.NS_DC } + XMPConst.NS_XML,
            actual = namespaces
        )
    }

    /**
     * Collects the paths of all properties the iterator reports.
     */
    private fun collectPaths(iterator: XMPIterator): List<String> {

        val paths = mutableListOf<String>()

        while (iterator.hasNext()) {
            val propertyInfo = iterator.next()
            paths.add(propertyInfo.getPath())
        }

        return paths
    }
}
