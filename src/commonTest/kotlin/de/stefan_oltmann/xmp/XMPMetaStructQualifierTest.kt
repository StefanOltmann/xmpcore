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
 * Tests the struct field and qualifier operations of [XMPMeta].
 */
class XMPMetaStructQualifierTest {

    /**
     * A struct field is created implicitly and read back.
     */
    @Test
    fun testSetAndGetStructField() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title", "Titel")

        assertTrue(xmpMeta.doesStructFieldExist(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title"))
        assertEquals(
            expected = "Titel",
            actual = checkNotNull(
                xmpMeta.getStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title")
            ).getValue()
        )
    }

    /**
     * A missing struct field reads back as null.
     */
    @Test
    fun testGetMissingStructFieldReturnsNull() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title"))
        assertFalse(xmpMeta.doesStructFieldExist(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title"))
    }

    /**
     * Deleting a struct field removes it.
     */
    @Test
    fun testDeleteStructField() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title", "Titel")
        xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "description", "Text")

        xmpMeta.deleteStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title")

        assertFalse(xmpMeta.doesStructFieldExist(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "title"))
        assertTrue(xmpMeta.doesStructFieldExist(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "description"))
    }

    /**
     * Invalid struct field parameters are rejected.
     */
    @Test
    fun testStructFieldInvalidParamsThrows() {

        val xmpMeta = XMPMetaFactory.create()

        assertFailsWith<XMPException> {
            xmpMeta.setStructField(XMPConst.NS_XMP, "struct", "", "title", "Titel")
        }.let { assertEquals(XMPErrorConst.BADSCHEMA, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "", "Titel")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setStructField(XMPConst.NS_XMP, "struct", XMPConst.NS_DC, "a/b", "Titel")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * Struct fields deep inside structs are addressable by path.
     */
    @Test
    fun testGetFieldOverDeepPath() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "outer/xmp:inner/xmp:leaf", "value")

        assertEquals(
            expected = "value",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP, "outer/xmp:inner/xmp:leaf")
        )
    }

    /**
     * A qualifier is set on an existing property and read back.
     */
    @Test
    fun testSetAndGetQualifier() {

        val qualifierNamespace = "http://example.org/xmpcore-qual/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "qualTest")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom", "qualValue")

        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom"))
        assertEquals(
            expected = "qualValue",
            actual = checkNotNull(
                xmpMeta.getQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom")
            ).getValue()
        )
    }

    /**
     * A missing qualifier reads back as null.
     */
    @Test
    fun testGetMissingQualifierReturnsNull() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")

        assertNull(xmpMeta.getQualifier(XMPConst.NS_XMP, "prop", XMPConst.NS_XML, "lang"))
        assertFalse(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "prop", XMPConst.NS_XML, "lang"))
    }

    /**
     * Deleting a qualifier removes it.
     */
    @Test
    fun testDeleteQualifier() {

        val qualifierNamespace = "http://example.org/xmpcore-qual-delete/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "qualDelete")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")
        xmpMeta.setQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom", "qualValue")

        xmpMeta.deleteQualifier(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom")

        assertFalse(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "prop", qualifierNamespace, "custom"))
    }

    /**
     * Setting a qualifier on a missing property is rejected.
     */
    @Test
    fun testSetQualifierOnMissingPropertyThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setQualifier(XMPConst.NS_XMP, "missing", XMPConst.NS_XML, "lang", "x-default")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Qualifiers can be attached to array items.
     */
    @Test
    fun testQualifierOnArrayItem() {

        val qualifierNamespace = "http://example.org/xmpcore-qual-item/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "qualItem")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArray(true),
            "fox"
        )

        xmpMeta.setQualifier(XMPConst.NS_DC, "subject[1]", qualifierNamespace, "custom", "v")

        assertEquals(
            expected = "v",
            actual = checkNotNull(
                xmpMeta.getQualifier(XMPConst.NS_DC, "subject[1]", qualifierNamespace, "custom")
            ).getValue()
        )
    }

    /**
     * The language qualifier is set via the xml:lang path and readable.
     */
    @Test
    fun testGetLangQualifierViaPath() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        assertEquals(
            expected = "x-default",
            actual = checkNotNull(
                xmpMeta.getQualifier(XMPConst.NS_DC, "title[1]", XMPConst.NS_XML, "lang")
            ).getValue()
        )
    }
}
