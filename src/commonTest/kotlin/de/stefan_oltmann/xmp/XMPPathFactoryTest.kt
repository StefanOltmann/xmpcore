package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the path composition functions of [XMPPathFactory].
 */
class XMPPathFactoryTest {

    /**
     * Positive indices render as plain array index paths.
     */
    @Test
    fun testComposeArrayItemPath() {

        assertEquals(
            expected = "subject[1]",
            actual = XMPPathFactory.composeArrayItemPath("subject", 1)
        )

        assertEquals(
            expected = "subject[2]",
            actual = XMPPathFactory.composeArrayItemPath("subject", 2)
        )
    }

    /**
     * [XMPConst.ARRAY_LAST_ITEM] renders as the last-item selector.
     */
    @Test
    fun testComposeArrayItemPathLastItem() {

        assertEquals(
            expected = "subject[last()]",
            actual = XMPPathFactory.composeArrayItemPath("subject", XMPConst.ARRAY_LAST_ITEM)
        )
    }

    /**
     * Indices below zero, other than ARRAY_LAST_ITEM, are rejected.
     */
    @Test
    fun testComposeArrayItemPathInvalidIndexThrows() {

        for (index in listOf(0, -2)) {

            val ex = assertFailsWith<XMPException> {
                XMPPathFactory.composeArrayItemPath("subject", index)
            }

            assertEquals(XMPErrorConst.BADINDEX, ex.errorCode)
        }
    }

    /**
     * A simple struct field name renders with its namespace prefix.
     */
    @Test
    fun testComposeStructFieldPath() {

        assertEquals(
            expected = "/dc:title",
            actual = XMPPathFactory.composeStructFieldPath(XMPConst.NS_DC, "title")
        )
    }

    /**
     * Empty or complex field names and empty namespaces are rejected.
     */
    @Test
    fun testComposeStructFieldPathInvalidThrows() {

        assertFailsWith<XMPException> {
            XMPPathFactory.composeStructFieldPath("", "title")
        }.let { assertEquals(XMPErrorConst.BADSCHEMA, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathFactory.composeStructFieldPath(XMPConst.NS_DC, "")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathFactory.composeStructFieldPath(XMPConst.NS_DC, "a/b")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * A simple qualifier name renders as a qualifier path step.
     */
    @Test
    fun testComposeQualifierPath() {

        assertEquals(
            expected = "/?xml:lang",
            actual = XMPPathFactory.composeQualifierPath(XMPConst.NS_XML, "lang")
        )
    }

    /**
     * Empty or complex qualifier names and empty namespaces are rejected.
     */
    @Test
    fun testComposeQualifierPathInvalidThrows() {

        assertFailsWith<XMPException> {
            XMPPathFactory.composeQualifierPath("", "lang")
        }.let { assertEquals(XMPErrorConst.BADSCHEMA, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathFactory.composeQualifierPath(XMPConst.NS_XML, "")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathFactory.composeQualifierPath(XMPConst.NS_XML, "a/b")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * The language selector normalizes the language tag.
     */
    @Test
    fun testComposeLangSelector() {

        assertEquals(
            expected = "title[?xml:lang=\"de\"]",
            actual = XMPPathFactory.composeLangSelector("title", "DE")
        )
    }

    /**
     * The field selector renders the field name with its namespace prefix.
     */
    @Test
    fun testComposeFieldSelector() {

        assertEquals(
            expected = "creators[dc:creator=\"John\"]",
            actual = XMPPathFactory.composeFieldSelector("creators", XMPConst.NS_DC, "creator", "John")
        )
    }

    /**
     * A complex field name in the field selector is rejected.
     */
    @Test
    fun testComposeFieldSelectorInvalidThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathFactory.composeFieldSelector("creators", XMPConst.NS_DC, "a/b", "John")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A name that expands to more than one step is rejected as field name.
     */
    @Test
    fun testComposeStructFieldPathWithArrayNameThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathFactory.composeStructFieldPath(XMPConst.NS_DC, "a[1]")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A name that expands to more than one step is rejected as qualifier name.
     */
    @Test
    fun testComposeQualifierPathWithArrayNameThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathFactory.composeQualifierPath(XMPConst.NS_DC, "a[1]")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A name that expands to more than one step is rejected as selector field.
     */
    @Test
    fun testComposeFieldSelectorWithArrayNameThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathFactory.composeFieldSelector("creators", XMPConst.NS_DC, "a[1]", "John")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }
}
