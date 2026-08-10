package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the array operations of [XMPMeta].
 */
class XMPMetaArraysTest {

    /**
     * Appending items creates the array implicitly and reads back in order.
     */
    @Test
    fun testAppendAndReadArrayItems() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("fox", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)!!.getValue())
        assertEquals("swiper", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)!!.getValue())
        assertTrue(xmpMeta.doesArrayItemExist(XMPConst.NS_DC, "subject", 1))
        assertFalse(xmpMeta.doesArrayItemExist(XMPConst.NS_DC, "subject", 3))
    }

    /**
     * [XMPConst.ARRAY_LAST_ITEM] addresses the last array item.
     */
    @Test
    fun testGetLastArrayItem() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")

        assertEquals(
            expected = "swiper",
            actual = xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", XMPConst.ARRAY_LAST_ITEM)!!.getValue()
        )
    }

    /**
     * An empty array created by [XMPMeta.setProperty] counts zero items.
     */
    @Test
    fun testCreateEmptyArrayViaSetProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null, PropertyOptions().setArray(true))

        assertEquals(0, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * [XMPMeta.countArrayItems] returns zero for a missing array.
     */
    @Test
    fun testCountArrayItemsOnMissingArray() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(0, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * [XMPMeta.countArrayItems] on a simple property is rejected.
     */
    @Test
    fun testCountArrayItemsOnSimplePropertyThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", "value")

        val ex = assertFailsWith<XMPException> {
            xmpMeta.countArrayItems(XMPConst.NS_DC, "subject")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPMeta.setArrayItem] replaces an existing item.
     */
    @Test
    fun testSetArrayItemReplaces() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")

        xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 1, "bird")

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("bird", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)!!.getValue())
    }

    /**
     * [XMPMeta.setArrayItem] with [XMPConst.ARRAY_LAST_ITEM] replaces the last item.
     */
    @Test
    fun testSetLastArrayItem() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")

        xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", XMPConst.ARRAY_LAST_ITEM, "bird")

        assertEquals("bird", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)!!.getValue())
    }

    /**
     * [XMPMeta.setArrayItem] on a missing array is rejected.
     */
    @Test
    fun testSetArrayItemOnMissingArrayThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 1, "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPMeta.insertArrayItem] inserts before the given index.
     */
    @Test
    fun testInsertArrayItemInMiddle() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")

        xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", 1, "bird")

        assertEquals("bird", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)!!.getValue())
        assertEquals("fox", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)!!.getValue())
        assertEquals(3, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * Out of range indexes are rejected for both setters.
     */
    @Test
    fun testOutOfRangeIndexesThrow() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")

        assertFailsWith<XMPException> {
            xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 2, "value")
        }.let { assertEquals(XMPErrorConst.BADINDEX, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 0, "value")
        }.let { assertEquals(XMPErrorConst.BADINDEX, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", 3, "value")
        }.let { assertEquals(XMPErrorConst.BADINDEX, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", 0, "value")
        }.let { assertEquals(XMPErrorConst.BADINDEX, it.errorCode) }
    }

    /**
     * [XMPMeta.insertArrayItem] with [XMPConst.ARRAY_LAST_ITEM] appends.
     */
    @Test
    fun testInsertLastArrayItemAppends() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")

        xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", XMPConst.ARRAY_LAST_ITEM, "swiper")

        assertEquals("swiper", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)!!.getValue())
    }

    /**
     * [XMPMeta.appendArrayItem] without explicit array options on a missing
     * array is rejected.
     */
    @Test
    fun testAppendWithoutArrayOptionsThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", itemValue = "fox")
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * Non-array option flags for a new array are rejected.
     */
    @Test
    fun testAppendWithInvalidArrayOptionsThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.appendArrayItem(
                XMPConst.NS_DC,
                "subject",
                PropertyOptions().setURI(true),
                "fox"
            )
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * Appending to a simple property is rejected.
     */
    @Test
    fun testAppendToSimplePropertyThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", "value")

        val ex = assertFailsWith<XMPException> {
            xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * [XMPMeta.deleteArrayItem] removes single items and the last item.
     */
    @Test
    fun testDeleteArrayItems() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "swiper")
        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "bird")

        xmpMeta.deleteArrayItem(XMPConst.NS_DC, "subject", 1)

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("swiper", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)!!.getValue())

        xmpMeta.deleteArrayItem(XMPConst.NS_DC, "subject", XMPConst.ARRAY_LAST_ITEM)

        assertEquals("swiper", xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", XMPConst.ARRAY_LAST_ITEM)!!.getValue())
        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * Deleting a missing array item does not fail.
     */
    @Test
    fun testDeleteMissingArrayItemDoesNotFail() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.deleteArrayItem(XMPConst.NS_DC, "subject", 1)
    }

    /**
     * Array items can be structs, and their fields are accessible.
     */
    @Test
    fun testArrayOfStructs() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "regions",
            PropertyOptions().setArray(true),
            itemValue = null,
            itemOptions = PropertyOptions().setStruct(true)
        )

        xmpMeta.setStructField(XMPConst.NS_XMP, "regions[1]", XMPConst.NS_XMP, "name", "Region A")

        assertEquals(
            expected = "Region A",
            actual = xmpMeta.getStructField(XMPConst.NS_XMP, "regions[1]", XMPConst.NS_XMP, "name")!!.getValue()
        )
        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_XMP, "regions"))
    }

    /**
     * A missing array item reads back as null.
     */
    @Test
    fun testGetMissingArrayItemReturnsNull() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1))
    }

    /**
     * An index below one is rejected for reading.
     */
    @Test
    fun testGetArrayItemWithInvalidIndexThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 0)
        }

        assertEquals(XMPErrorConst.BADINDEX, ex.errorCode)
    }

    /**
     * [XMPMeta.insertArrayItem] on a missing array is rejected.
     */
    @Test
    fun testInsertArrayItemOnMissingArrayThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", 1, "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An array index of zero in a path expression is rejected.
     */
    @Test
    fun testSetPropertyWithZeroIndexThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null, PropertyOptions().setArray(true))

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_DC, "subject[0]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An overflowing array index is rejected.
     */
    @Test
    fun testSetPropertyWithHugeIndexThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null, PropertyOptions().setArray(true))

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_DC, "subject[99999999999999999999999]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A field selector addresses an item in an array of structs.
     */
    @Test
    fun testFieldSelectorOnArrayOfStructs() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "creators",
            PropertyOptions().setArray(true),
            itemValue = null,
            itemOptions = PropertyOptions().setStruct(true)
        )
        xmpMeta.setStructField(XMPConst.NS_XMP, "creators[1]", XMPConst.NS_DC, "name", "John")
        xmpMeta.setStructField(XMPConst.NS_XMP, "creators[1]", XMPConst.NS_DC, "url", "http://john")

        xmpMeta.appendArrayItem(
            XMPConst.NS_XMP,
            "creators",
            PropertyOptions().setArray(true),
            itemValue = null,
            itemOptions = PropertyOptions().setStruct(true)
        )
        xmpMeta.setStructField(XMPConst.NS_XMP, "creators[2]", XMPConst.NS_DC, "name", "Jane")
        xmpMeta.setStructField(XMPConst.NS_XMP, "creators[2]", XMPConst.NS_DC, "url", "http://jane")

        assertEquals(
            expected = "http://jane",
            actual = xmpMeta.getStructField(
                XMPConst.NS_XMP,
                "creators[dc:name='Jane']",
                XMPConst.NS_DC,
                "url"
            )!!.getValue()
        )
    }

    /**
     * A field selector on an array of simple items is rejected.
     */
    @Test
    fun testFieldSelectorOnSimpleArrayThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(XMPConst.NS_DC, "subject", PropertyOptions().setArray(true), "fox")

        val ex = assertFailsWith<XMPException> {
            xmpMeta.getPropertyString(XMPConst.NS_DC, "subject[dc:name='fox']")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }
}
