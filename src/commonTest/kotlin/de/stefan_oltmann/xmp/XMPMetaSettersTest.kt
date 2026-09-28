package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the behavior of the array item setter methods of [XMPMeta].
 */
class XMPMetaSettersTest {

    /**
     * Setting an item within the bounds of an existing array replaces that item.
     */
    @Test
    fun testSetArrayItemReplacesExistingItem() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "oldValue"
        )

        xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 1, "newValue")

        assertEquals(
            expected = "newValue",
            actual = checkNotNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)).getValue()
        )
        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * Setting an item past the end of an existing array is rejected.
     */
    @Test
    fun testSetArrayItemPastTheEndThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "oldValue"
        )

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setArrayItem(XMPConst.NS_DC, "subject", 2, "newValue")
        }

        assertEquals(XMPErrorConst.BADINDEX, ex.errorCode)
    }

    /**
     * Inserting an item at the index after the last item appends it to the array.
     */
    @Test
    fun testInsertArrayItemAppendsPastTheEnd() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "first"
        )

        xmpMeta.insertArrayItem(XMPConst.NS_DC, "subject", 2, "second")

        assertEquals(
            expected = "second",
            actual = checkNotNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 2)).getValue()
        )
        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }

    /**
     * Setting a null value on an existing composite node whose requested form differs
     * clears the children instead of failing, like the Adobe original does.
     */
    @Test
    fun testSetPropertyNullValueOnExistingArrayClearsChildren() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:subject>
                  <rdf:Bag>
                    <rdf:li>fox</rdf:li>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setProperty(XMPConst.NS_DC, "subject", null)

        assertEquals(0, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
    }
}
