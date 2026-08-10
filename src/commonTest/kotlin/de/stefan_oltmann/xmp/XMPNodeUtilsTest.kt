package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the qualifier selector path form `[?qualName="value"]` on arrays.
 */
class XMPNodeUtilsTest {

    private val qualifierNamespace = "http://example.org/xmpcore-test/"

    /**
     * A qualifier selector matches an item in the middle of an array.
     */
    @Test
    fun testQualifierSelectorMatchesMiddleItem() {

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "test")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "first"
        )
        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "second"
        )
        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "third"
        )

        xmpMeta.setQualifier(XMPConst.NS_XMP, "subject[2]", qualifierNamespace, "custom", "value")

        assertEquals(
            expected = "second",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "subject[?test:custom='value']")
        )
    }

    /**
     * A qualifier selector matches the last item of an array.
     */
    @Test
    fun testQualifierSelectorMatchesLastItem() {

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "test")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "first"
        )
        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "second"
        )

        xmpMeta.setQualifier(XMPConst.NS_XMP, "subject[2]", qualifierNamespace, "custom", "value")

        assertEquals(
            expected = "second",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "subject[?test:custom='value']")
        )
    }

    /**
     * A qualifier selector matches the only item of a single-item array.
     */
    @Test
    fun testQualifierSelectorMatchesSingleItem() {

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "test")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "first"
        )

        xmpMeta.setQualifier(XMPConst.NS_XMP, "subject[1]", qualifierNamespace, "custom", "value")

        assertEquals(
            expected = "first",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "subject[?test:custom='value']")
        )
    }

    /**
     * A qualifier selector without a matching item yields no property.
     */
    @Test
    fun testQualifierSelectorWithoutMatchYieldsNothing() {

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "test")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "first"
        )

        xmpMeta.setQualifier(XMPConst.NS_XMP, "subject[1]", qualifierNamespace, "custom", "value")

        assertNull(
            xmpMeta.getPropertyString(XMPConst.NS_XMP, "subject[?test:custom='other']")
        )
    }

    /**
     * An xml:lang qualifier selector without a matching language yields no property.
     */
    @Test
    fun testLangQualifierSelectorWithoutMatchYieldsNothing() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        assertNull(
            xmpMeta.getPropertyString(XMPConst.NS_DC, "title[?xml:lang='fr']")
        )
    }
}
