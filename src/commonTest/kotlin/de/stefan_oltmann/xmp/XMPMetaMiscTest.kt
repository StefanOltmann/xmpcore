package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests miscellaneous edge cases of [XMPMeta] path handling.
 */
class XMPMetaMiscTest {

    /**
     * Setting a property via `[last()]` on an existing empty array reports that the
     * property does not exist instead of crashing.
     */
    @Test
    fun testSetPropertyWithLastItemOnEmptyArray() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null, PropertyOptions().setArrayOrdered(true))

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_DC, "subject[last()]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Setting a property with an out-of-range index removes the implicitly
     * created nodes and reports the error.
     */
    @Test
    fun testSetPropertyWithOutOfRangeIndexThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_DC, "subject[3]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An out-of-range index on an alias-created array removes the implicitly
     * created schema and array nodes.
     */
    @Test
    fun testSetPropertyWithOutOfRangeIndexOnAliasCleansUp() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            /* The pdf:Title alias would create dc:title as alt-text array. */
            xmpMeta.setProperty(XMPConst.NS_PDF, "Title[3]", "value")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
        assertFalse(xmpMeta.doesPropertyExist(XMPConst.NS_DC, "title"))
    }
}
