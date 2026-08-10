package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XMPSchemaRegistry
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the node finding, options verification and localized text helpers of [XMPNodeUtils].
 */
class XMPNodeUtilsFeaturesTest {

    private val testNamespace = "http://example.org/xmpcore-nodeutils/"

    /**
     * A missing schema node is created when requested.
     */
    @Test
    fun testFindSchemaNodeCreatesNode() {

        XMPSchemaRegistry.registerNamespace(testNamespace, "nuTest")

        val root = XMPNode("root", null)

        val schemaNode = XMPNodeUtils.findSchemaNode(root, testNamespace, true)

        assertEquals(testNamespace, schemaNode?.name)
        assertSame(schemaNode, root.findChildByName(testNamespace))
    }

    /**
     * Creating a schema node with an unregistered namespace and no suggested
     * prefix is rejected.
     */
    @Test
    fun testFindSchemaNodeUnregisteredThrows() {

        val root = XMPNode("root", null)

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.findSchemaNode(root, "http://example.org/not-registered/", null, true)
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * A suggested prefix registers the unregistered namespace.
     */
    @Test
    fun testFindSchemaNodeWithSuggestedPrefix() {

        val namespace = "http://example.org/xmpcore-nodeutils-suggest/"

        val root = XMPNode("root", null)

        val schemaNode = XMPNodeUtils.findSchemaNode(root, namespace, "nuSuggest", true)

        assertEquals("nuSuggest:", XMPSchemaRegistry.getNamespacePrefix(namespace))
        assertEquals(namespace, schemaNode?.name)
    }

    /**
     * Without creation only an existing schema node is returned.
     */
    @Test
    fun testFindSchemaNodeWithoutCreation() {

        val namespace = "http://example.org/xmpcore-nodeutils-nocreate/"

        XMPSchemaRegistry.registerNamespace(namespace, "nuNoCreate")

        val root = XMPNode("root", null)

        assertNull(XMPNodeUtils.findSchemaNode(root, namespace, false))

        XMPNodeUtils.findSchemaNode(root, namespace, true)

        assertEquals(namespace, XMPNodeUtils.findSchemaNode(root, namespace, false)?.name)
    }

    /**
     * A null or empty XMPPath is rejected.
     */
    @Test
    fun testFindNodeWithEmptyPathThrows() {

        val root = XMPNode("root", null)

        assertFailsWith<XMPException> {
            XMPNodeUtils.findNode(root, null, false, null)
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPNodeUtils.findNode(root, XMPPath(), false, null)
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * Named children are only allowed below schemas and structs.
     */
    @Test
    fun testFindChildNodeOnSimpleNodeThrows() {

        val parent = XMPNode("prop", "value")

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.findChildNode(parent, "child", false)
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Named children are not allowed below arrays.
     */
    @Test
    fun testFindChildNodeOnArrayThrows() {

        val parent = XMPNode("arr", null, PropertyOptions(PropertyOptions.ARRAY))
        parent.isImplicit = true

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.findChildNode(parent, "child", true)
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An implicit simple node is promoted to a struct when a child is created.
     */
    @Test
    fun testFindChildNodePromotesImplicitNodeToStruct() {

        val parent = XMPNode("prop", null)
        parent.isImplicit = true

        val child = XMPNodeUtils.findChildNode(parent, "child", true)

        assertTrue(parent.options.isStruct())
        assertEquals("child", child?.name)
    }

    /**
     * Deleting a qualifier removes it from its parent.
     */
    @Test
    fun testDeleteNodeRemovesQualifier() {

        val parent = XMPNode("prop", "value")
        val qualifier = XMPNode("?custom", "v")
        parent.addQualifier(qualifier)

        XMPNodeUtils.deleteNode(qualifier)

        assertFalse(parent.hasQualifier())
    }

    /**
     * Deleting the last property of a schema removes the empty schema node.
     */
    @Test
    fun testDeleteNodeRemovesEmptySchema() {

        val root = XMPNode("root", null)
        val schema = XMPNode(testNamespace, null, PropertyOptions().setSchemaNode(true))
        val property = XMPNode("dc:prop", "value")
        root.addChild(schema)
        schema.addChild(property)

        XMPNodeUtils.deleteNode(property)

        assertFalse(root.hasChildren())
    }

    /**
     * [XMPNodeUtils.verifySetOptions] promotes the alt-text bits down to the
     * array bit.
     */
    @Test
    fun testVerifySetOptionsPromotesArrayBits() {

        val options = XMPNodeUtils.verifySetOptions(PropertyOptions().setArrayAltText(true), null)

        assertTrue(options.isArray())
        assertTrue(options.isArrayOrdered())
        assertTrue(options.isArrayAlternate())
        assertTrue(options.isArrayAltText())
    }

    /**
     * Composite options with a non-empty value are rejected.
     */
    @Test
    fun testVerifySetOptionsCompositeWithValueThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.verifySetOptions(PropertyOptions().setArray(true), "value")
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * [XMPNodeUtils.serializeNodeValue] converts the supported value types.
     */
    @Test
    fun testSerializeNodeValue() {

        assertEquals("True", XMPNodeUtils.serializeNodeValue(true))
        assertEquals("False", XMPNodeUtils.serializeNodeValue(false))
        assertEquals("42", XMPNodeUtils.serializeNodeValue(42))
        assertEquals("42", XMPNodeUtils.serializeNodeValue(42L))
        assertEquals("3.5", XMPNodeUtils.serializeNodeValue(3.5))
        assertEquals("AQID", XMPNodeUtils.serializeNodeValue(byteArrayOf(1, 2, 3)))
        assertEquals("text", XMPNodeUtils.serializeNodeValue("text"))
        assertNull(XMPNodeUtils.serializeNodeValue(null))
    }

    /**
     * Control chars in node values are replaced with spaces.
     */
    @Test
    fun testSerializeNodeValueReplacesControlChars() {

        assertEquals("a b", XMPNodeUtils.serializeNodeValue("a\u0000b"))
    }

    /**
     * Language qualifier values are normalized when set.
     */
    @Test
    fun testSetNodeValueNormalizesLanguage() {

        val node = XMPNode(
            XMPConst.XML_LANG,
            null,
            PropertyOptions().setQualifier(true)
        )

        XMPNodeUtils.setNodeValue(node, "DE-de")

        assertEquals("de-DE", node.value)
    }

    /**
     * Non-language values are stored unchanged.
     */
    @Test
    fun testSetNodeValueKeepsValue() {

        val node = XMPNode("dc:prop", null)

        XMPNodeUtils.setNodeValue(node, "42")

        assertEquals("42", node.value)
    }

    /**
     * [XMPNodeUtils.normalizeLangArray] moves the x-default item to the front
     * and copies its value to the item it displaced.
     */
    @Test
    fun testNormalizeLangArrayMovesXDefaultToFront() {

        val array = langArray(
            langItem("de", "Deutsch"),
            langItem("x-default", "Default"),
            langItem("fr", "Francais")
        )

        XMPNodeUtils.normalizeLangArray(array)

        assertEquals("x-default", array.getChild(1).getQualifier(1).value)
        assertEquals("Default", array.getChild(1).value)
        /* The displaced former first item gets the x-default value. */
        assertEquals("Default", array.getChild(2).value)
        assertEquals("fr", array.getChild(3).getQualifier(1).value)
    }

    /**
     * [XMPNodeUtils.detectAltText] marks an alternate array with language items
     * as alt-text array.
     */
    @Test
    fun testDetectAltText() {

        val array = XMPNode(
            "dc:title",
            null,
            PropertyOptions(
                PropertyOptions.ARRAY or PropertyOptions.ARRAY_ORDERED or PropertyOptions.ARRAY_ALTERNATE
            )
        )
        array.addChild(langItem("x-default", "Default"))

        XMPNodeUtils.detectAltText(array)

        assertTrue(array.options.isArrayAltText())
    }

    /**
     * [XMPNodeUtils.appendLangItem] adds the x-default item at the front.
     */
    @Test
    fun testAppendLangItemXDefaultAtFront() {

        val array = langArray(langItem("de", "Deutsch"))

        XMPNodeUtils.appendLangItem(array, "x-default", "Default")

        assertEquals(2, array.getChildrenLength())
        assertEquals("x-default", array.getChild(1).getQualifier(1).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] reports no values for an empty array.
     */
    @Test
    fun testChooseLocalizedTextNoValues() {

        val array = langArray()

        val result = XMPNodeUtils.chooseLocalizedText(array, null, "de")

        assertEquals(XMPNodeUtils.CLT_NO_VALUES, result[0])
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] finds an exact specific language match.
     */
    @Test
    fun testChooseLocalizedTextSpecificMatch() {

        val array = langArray(
            langItem("de", "Deutsch"),
            langItem("de-de", "Deutschland")
        )

        val result = XMPNodeUtils.chooseLocalizedText(array, null, "de-de")

        assertEquals(XMPNodeUtils.CLT_SPECIFIC_MATCH, result[0])
        assertEquals("Deutschland", (result[1] as XMPNode).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] reports a single generic language match.
     */
    @Test
    fun testChooseLocalizedTextSingleGenericMatch() {

        val array = langArray(
            langItem("de-de", "Deutschland"),
            langItem("fr", "France")
        )

        val result = XMPNodeUtils.chooseLocalizedText(array, "de", "de-at")

        assertEquals(XMPNodeUtils.CLT_SINGLE_GENERIC, result[0])
        assertEquals("Deutschland", (result[1] as XMPNode).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] reports multiple generic language matches.
     */
    @Test
    fun testChooseLocalizedTextMultipleGenericMatches() {

        val array = langArray(
            langItem("de-de", "Deutschland"),
            langItem("de-at", "Oesterreich")
        )

        val result = XMPNodeUtils.chooseLocalizedText(array, "de", "de-ch")

        assertEquals(XMPNodeUtils.CLT_MULTIPLE_GENERIC, result[0])
        assertEquals("Deutschland", (result[1] as XMPNode).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] falls back to the x-default item.
     */
    @Test
    fun testChooseLocalizedTextXDefaultFallback() {

        val array = langArray(
            langItem("x-default", "Default"),
            langItem("fr", "France")
        )

        val result = XMPNodeUtils.chooseLocalizedText(array, null, "de")

        assertEquals(XMPNodeUtils.CLT_XDEFAULT, result[0])
        assertEquals("Default", (result[1] as XMPNode).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] falls back to the first item.
     */
    @Test
    fun testChooseLocalizedTextFirstItemFallback() {

        val array = langArray(
            langItem("en", "England"),
            langItem("fr", "France")
        )

        val result = XMPNodeUtils.chooseLocalizedText(array, null, "de")

        assertEquals(XMPNodeUtils.CLT_FIRST_ITEM, result[0])
        assertEquals("England", (result[1] as XMPNode).value)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] rejects arrays that are not alt-text.
     */
    @Test
    fun testChooseLocalizedTextNotAltTextThrows() {

        val array = XMPNode(
            "dc:title",
            null,
            PropertyOptions(PropertyOptions.ARRAY or PropertyOptions.ARRAY_ORDERED)
        )

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.chooseLocalizedText(array, null, "de")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] rejects composite array items.
     */
    @Test
    fun testChooseLocalizedTextCompositeItemThrows() {

        val item = XMPNode(XMPConst.ARRAY_ITEM_NAME, null, PropertyOptions().setStruct(true))
        item.addQualifier(XMPNode(XMPConst.XML_LANG, "de"))

        val array = langArray(item)

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.chooseLocalizedText(array, null, "de")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPNodeUtils.chooseLocalizedText] rejects items without a language qualifier.
     */
    @Test
    fun testChooseLocalizedTextMissingLangQualifierThrows() {

        val item = XMPNode(XMPConst.ARRAY_ITEM_NAME, "value")

        val array = langArray(item)

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.chooseLocalizedText(array, null, "de")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPNodeUtils.lookupLanguageItem] finds the index of a language item.
     */
    @Test
    fun testLookupLanguageItem() {

        val array = langArray(
            langItem("x-default", "Default"),
            langItem("de", "Deutsch")
        )

        assertEquals(2, XMPNodeUtils.lookupLanguageItem(array, "de"))
        assertEquals(-1, XMPNodeUtils.lookupLanguageItem(array, "fr"))
    }

    /**
     * [XMPNodeUtils.lookupLanguageItem] rejects non-array nodes.
     */
    @Test
    fun testLookupLanguageItemOnNonArrayThrows() {

        val node = XMPNode("dc:prop", "value")

        val ex = assertFailsWith<XMPException> {
            XMPNodeUtils.lookupLanguageItem(node, "de")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Creates an alt-text array node with the given language items.
     */
    private fun langArray(vararg items: XMPNode): XMPNode {

        val array = XMPNode(
            "dc:title",
            null,
            PropertyOptions(
                PropertyOptions.ARRAY or PropertyOptions.ARRAY_ORDERED or
                    PropertyOptions.ARRAY_ALTERNATE or PropertyOptions.ARRAY_ALT_TEXT
            )
        )

        for (item in items)
            array.addChild(item)

        return array
    }

    /**
     * Creates an array item with a language qualifier.
     */
    private fun langItem(lang: String?, value: String): XMPNode {

        val item = XMPNode(XMPConst.ARRAY_ITEM_NAME, value)

        item.addQualifier(XMPNode(XMPConst.XML_LANG, lang))

        return item
    }
}
