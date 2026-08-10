package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the tree structure operations of [XMPNode].
 */
class XMPNodeFeaturesTest {

    /**
     * Adding a child with an existing name is ignored.
     */
    @Test
    fun testAddChildIgnoresDuplicateNames() {

        val parent = XMPNode("parent", null)

        parent.addChild(XMPNode("child", "first"))
        parent.addChild(XMPNode("child", "second"))

        assertEquals(1, parent.getChildrenLength())
        assertEquals("first", parent.getChild(1).value)
    }

    /**
     * Array items with the repeated name "[]" are always added.
     */
    @Test
    fun testAddChildAllowsDuplicateArrayItems() {

        val parent = XMPNode("arr", null, PropertyOptions().setArray(true))

        parent.addChild(XMPNode(XMPConst.ARRAY_ITEM_NAME, "first"))
        parent.addChild(XMPNode(XMPConst.ARRAY_ITEM_NAME, "second"))

        assertEquals(2, parent.getChildrenLength())
    }

    /**
     * Adding an existing qualifier is ignored.
     */
    @Test
    fun testAddQualifierIgnoresDuplicates() {

        val parent = XMPNode("prop", "value")

        parent.addQualifier(XMPNode("?custom", "first"))
        parent.addQualifier(XMPNode("?custom", "second"))

        assertEquals(1, parent.getQualifierLength())
        assertEquals("first", parent.getQualifier(1).value)
    }

    /**
     * The xml:lang qualifier is always first, rdf:type second.
     */
    @Test
    fun testAddQualifierOrdersLangAndType() {

        val parent = XMPNode("prop", "value")

        parent.addQualifier(XMPNode("?custom", "c"))
        parent.addQualifier(XMPNode(XMPConst.RDF_TYPE, "t"))
        parent.addQualifier(XMPNode(XMPConst.XML_LANG, "de"))

        assertEquals(XMPConst.XML_LANG, parent.getQualifier(1).name)
        assertEquals(XMPConst.RDF_TYPE, parent.getQualifier(2).name)
        assertEquals("?custom", parent.getQualifier(3).name)
        assertTrue(parent.options.hasLanguage())
        assertTrue(parent.options.hasType())
    }

    /**
     * Removing the xml:lang qualifier clears the hasLanguage flag.
     */
    @Test
    fun testRemoveQualifierClearsFlags() {

        val parent = XMPNode("prop", "value")

        val lang = XMPNode(XMPConst.XML_LANG, "de")
        parent.addQualifier(lang)

        parent.removeQualifier(lang)

        assertFalse(parent.options.hasLanguage())
        assertFalse(parent.hasQualifier())
    }

    /**
     * Removing the rdf:type qualifier clears the hasType flag.
     */
    @Test
    fun testRemoveTypeQualifierClearsFlag() {

        val parent = XMPNode("prop", "value")

        val type = XMPNode(XMPConst.RDF_TYPE, "t")
        parent.addQualifier(type)

        parent.removeQualifier(type)

        assertFalse(parent.options.hasType())
    }

    /**
     * [XMPNode.removeQualifiers] removes all qualifiers and flags.
     */
    @Test
    fun testRemoveQualifiers() {

        val parent = XMPNode("prop", "value")

        parent.addQualifier(XMPNode(XMPConst.XML_LANG, "de"))
        parent.addQualifier(XMPNode("?custom", "c"))

        parent.removeQualifiers()

        assertFalse(parent.hasQualifier())
        assertFalse(parent.options.hasQualifiers())
        assertFalse(parent.options.hasLanguage())
    }

    /**
     * [XMPNode.replaceChild] replaces the child at the given index.
     */
    @Test
    fun testReplaceChild() {

        val parent = XMPNode("parent", null)

        parent.addChild(XMPNode("child", "old"))

        val replacement = XMPNode("child", "new")

        parent.replaceChild(1, replacement)

        assertEquals(1, parent.getChildrenLength())
        assertEquals("new", parent.getChild(1).value)
        assertSame(parent, replacement.parent)
    }

    /**
     * [XMPNode.clear] resets the node completely.
     */
    @Test
    fun testClearResetsNode() {

        val node = XMPNode("child", "value", PropertyOptions().setStruct(true))
        node.addChild(XMPNode("field", "v"))
        node.addQualifier(XMPNode("?custom", "c"))

        node.clear()

        assertNull(node.name)
        assertNull(node.value)
        assertFalse(node.hasChildren())
        assertFalse(node.hasQualifier())
        assertEquals(PropertyOptions(), node.options)
    }

    /**
     * [XMPNode.removeChildren] removes all children.
     */
    @Test
    fun testRemoveChildren() {

        val parent = XMPNode("parent", null)

        parent.addChild(XMPNode("first", "1"))
        parent.addChild(XMPNode("second", "2"))

        parent.removeChildren()

        assertEquals(0, parent.getChildrenLength())
        assertFalse(parent.hasChildren())
    }

    /**
     * The child lookups find nodes by name.
     */
    @Test
    fun testFindByName() {

        val parent = XMPNode("parent", null)

        val child = XMPNode("child", "v")
        val qualifier = XMPNode("?custom", "c")

        parent.addChild(child)
        parent.addQualifier(qualifier)

        assertSame(child, parent.findChildByName("child"))
        assertNull(parent.findChildByName("missing"))
        assertSame(qualifier, parent.findQualifierByName("?custom"))
        assertNull(parent.findQualifierByName("missing"))
    }

    /**
     * Schema nodes compare by their prefix, other nodes by their name.
     */
    @Test
    fun testCompareTo() {

        val schemaA = XMPNode("uri-a", "a:", PropertyOptions().setSchemaNode(true))
        val schemaB = XMPNode("uri-b", "b:", PropertyOptions().setSchemaNode(true))

        assertTrue(schemaA < schemaB)

        val propA = XMPNode("a:prop", null)
        val propB = XMPNode("b:prop", null)

        assertTrue(propA < propB)
    }
}
