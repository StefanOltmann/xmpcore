package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests the parameter validation of the [XMPMeta] API.
 */
class XMPMetaParamErrorsTest {

    private val xmpMeta = XMPMetaFactory.create()

    /**
     * The array operations reject empty schema and array names.
     */
    @Test
    fun testArrayOperationsWithEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.getArrayItem("", "subject", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getArrayItem(XMPConst.NS_DC, "", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.countArrayItems("", "subject")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.countArrayItems(XMPConst.NS_DC, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setArrayItem("", "subject", 1, "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.insertArrayItem("", "subject", 1, "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.appendArrayItem("", "subject", PropertyOptions().setArray(true), "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteArrayItem(XMPConst.NS_DC, "", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesArrayItemExist(XMPConst.NS_DC, "", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The property deletion and existence checks reject empty names.
     */
    @Test
    fun testDeleteAndExistsWithEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.deleteProperty("", "prop")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteProperty(XMPConst.NS_XMP, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesPropertyExist("", "prop")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesPropertyExist(XMPConst.NS_XMP, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The struct field operations reject empty schema and struct names.
     */
    @Test
    fun testStructFieldOperationsWithEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.setStructField("", "struct", XMPConst.NS_DC, "field", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setStructField(XMPConst.NS_XMP, "", XMPConst.NS_DC, "field", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getStructField("", "struct", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteStructField(XMPConst.NS_XMP, "", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesStructFieldExist(XMPConst.NS_XMP, "", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The qualifier operations reject empty schema and property names.
     */
    @Test
    fun testQualifierOperationsWithEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.setQualifier("", "prop", XMPConst.NS_XML, "lang", "x-default")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setQualifier(XMPConst.NS_XMP, "", XMPConst.NS_XML, "lang", "x-default")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getQualifier("", "prop", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getQualifier(XMPConst.NS_XMP, "", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteQualifier("", "prop", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The localized text operations reject empty schema and array names.
     */
    @Test
    fun testLocalizedTextWithEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.getLocalizedText("", "title", null, "de")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getLocalizedText(XMPConst.NS_DC, "", null, "de")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setLocalizedText("", "title", null, "de", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setLocalizedText(XMPConst.NS_DC, "", null, "de", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The remaining empty parameter combinations are rejected too.
     */
    @Test
    fun testRemainingEmptyParamsThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.getProperty(XMPConst.NS_XMP, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getStructField(XMPConst.NS_XMP, "", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteArrayItem("", "subject", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesArrayItemExist("", "subject", 1)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.deleteStructField("", "struct", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesStructFieldExist("", "struct", XMPConst.NS_DC, "field")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesQualifierExist("", "prop", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The array item setters reject an empty array name.
     */
    @Test
    fun testArrayItemSettersWithEmptyArrayNameThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.setArrayItem(XMPConst.NS_DC, "", 1, "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.insertArrayItem(XMPConst.NS_DC, "", 1, "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.appendArrayItem(XMPConst.NS_DC, "", PropertyOptions().setArray(true), "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The qualifier operations reject an empty property name.
     */
    @Test
    fun testQualifierOperationsWithEmptyPropertyNameThrow() {

        assertFailsWith<XMPException> {
            xmpMeta.deleteQualifier(XMPConst.NS_XMP, "", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "", XMPConst.NS_XML, "lang")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }
}
