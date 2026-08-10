package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests the typed property getters and setters of [XMPMeta] and their value conversions.
 */
class XMPMetaTypedValuesTest {

    /**
     * Boolean values are stored as the canonical strings and read back.
     */
    @Test
    fun testSetAndGetBooleanProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyBoolean(XMPConst.NS_XMP, "flag", true)

        assertEquals(true, xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag"))
        assertEquals("True", xmpMeta.getPropertyString(XMPConst.NS_XMP, "flag"))

        xmpMeta.setPropertyBoolean(XMPConst.NS_XMP, "flag", false)

        assertEquals(false, xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag"))
        assertEquals("False", xmpMeta.getPropertyString(XMPConst.NS_XMP, "flag"))
    }

    /**
     * The common textual true values convert to true.
     */
    @Test
    fun testGetBooleanAcceptsCommonTrueValues() {

        val xmpMeta = XMPMetaFactory.create()

        for ((index, value) in listOf("True", "true", "t", "T", "on", "yes", "1", "-3").withIndex()) {

            xmpMeta.setProperty(XMPConst.NS_XMP, "flag$index", value)

            assertEquals(
                expected = true,
                actual = xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag$index"),
                message = "value: $value"
            )
        }
    }

    /**
     * The common textual false values convert to false.
     */
    @Test
    fun testGetBooleanAcceptsCommonFalseValues() {

        val xmpMeta = XMPMetaFactory.create()

        for ((index, value) in listOf("False", "false", "f", "off", "no", "0").withIndex()) {

            xmpMeta.setProperty(XMPConst.NS_XMP, "flag$index", value)

            assertEquals(
                expected = false,
                actual = xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag$index"),
                message = "value: $value"
            )
        }
    }

    /**
     * An unrecognized non-numeric value converts to false instead of failing.
     */
    @Test
    fun testGetBooleanFallbackForUnknownValue() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "flag", "xyz")

        assertEquals(false, xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag"))
    }

    /**
     * An empty value cannot be converted to a boolean.
     */
    @Test
    fun testGetBooleanOnEmptyValueThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "flag", "")

        val ex = assertFailsWith<XMPException> {
            xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "flag")
        }

        assertEquals(XMPErrorConst.BADVALUE, ex.errorCode)
    }

    /**
     * Integer values, including hex strings, convert correctly.
     */
    @Test
    fun testGetAndSetIntegerProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyInteger(XMPConst.NS_XMP, "value", 42)

        assertEquals(42, xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "value"))

        xmpMeta.setProperty(XMPConst.NS_XMP, "hex", "0x1F")

        assertEquals(31, xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "hex"))
    }

    /**
     * Invalid integer strings are rejected.
     */
    @Test
    fun testGetIntegerInvalidValueThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "value", "abc")

        assertFailsWith<XMPException> {
            xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "value")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * Long values beyond the int range convert correctly.
     */
    @Test
    fun testGetAndSetLongProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyLong(XMPConst.NS_XMP, "value", 12_345_678_901_234L)

        assertEquals(12_345_678_901_234L, xmpMeta.getPropertyLong(XMPConst.NS_XMP, "value"))
    }

    /**
     * Double values convert correctly.
     */
    @Test
    fun testGetAndSetDoubleProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setPropertyDouble(XMPConst.NS_XMP, "value", 3.5)

        assertEquals(3.5, xmpMeta.getPropertyDouble(XMPConst.NS_XMP, "value"))
        assertEquals("3.5", xmpMeta.getPropertyString(XMPConst.NS_XMP, "value"))
    }

    /**
     * Byte arrays are stored as base64 and read back.
     */
    @Test
    fun testSetAndGetBase64Property() {

        val xmpMeta = XMPMetaFactory.create()

        val bytes = byteArrayOf(1, 2, 3, 4)

        xmpMeta.setPropertyBase64(XMPConst.NS_XMP, "value", bytes)

        assertContentEquals(bytes, xmpMeta.getPropertyBase64(XMPConst.NS_XMP, "value"))
        assertEquals("AQIDBA==", xmpMeta.getPropertyString(XMPConst.NS_XMP, "value"))
    }

    /**
     * An invalid base64 string is rejected.
     */
    @Test
    fun testGetBase64InvalidValueThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "value", "!!!")

        assertFailsWith<XMPException> {
            xmpMeta.getPropertyBase64(XMPConst.NS_XMP, "value")
        }.let { assertEquals(XMPErrorConst.BADVALUE, it.errorCode) }
    }

    /**
     * The value type is detected automatically when setting a property.
     */
    @Test
    fun testSetPropertyDetectsValueType() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "bool", true)
        xmpMeta.setProperty(XMPConst.NS_XMP, "int", 42)
        xmpMeta.setProperty(XMPConst.NS_XMP, "long", 42L)
        xmpMeta.setProperty(XMPConst.NS_XMP, "double", 1.5)
        xmpMeta.setProperty(XMPConst.NS_XMP, "bytes", byteArrayOf(1, 2))
        xmpMeta.setProperty(XMPConst.NS_XMP, "custom", CustomToString())

        assertEquals("True", xmpMeta.getPropertyString(XMPConst.NS_XMP, "bool"))
        assertEquals("42", xmpMeta.getPropertyString(XMPConst.NS_XMP, "int"))
        assertEquals("42", xmpMeta.getPropertyString(XMPConst.NS_XMP, "long"))
        assertEquals("1.5", xmpMeta.getPropertyString(XMPConst.NS_XMP, "double"))
        assertEquals("AQI=", xmpMeta.getPropertyString(XMPConst.NS_XMP, "bytes"))
        assertEquals("custom-value", xmpMeta.getPropertyString(XMPConst.NS_XMP, "custom"))
    }

    /**
     * A simple property with a null value reads back as empty string.
     */
    @Test
    fun testGetPropertyStringOnValueLessSimpleProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "empty", null)

        assertEquals("", xmpMeta.getPropertyString(XMPConst.NS_XMP, "empty"))
    }

    /**
     * A composite property has no string value.
     */
    @Test
    fun testGetPropertyStringOnCompositeProperty() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "array", null, PropertyOptions().setArray(true))

        assertNull(xmpMeta.getPropertyString(XMPConst.NS_XMP, "array"))
    }

    /**
     * A typed getter on a composite property is rejected.
     */
    @Test
    fun testGetTypedValueOnCompositePropertyThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "array", null, PropertyOptions().setArray(true))

        val ex = assertFailsWith<XMPException> {
            xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "array")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * All typed getters return null for missing properties.
     */
    @Test
    fun testTypedGettersReturnNullForMissingProperty() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getPropertyBoolean(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getPropertyLong(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getPropertyDouble(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getPropertyBase64(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getPropertyString(XMPConst.NS_XMP, "missing"))
        assertNull(xmpMeta.getProperty(XMPConst.NS_XMP, "missing"))
    }

    /**
     * Setting a composite property with a non-empty value is rejected.
     */
    @Test
    fun testSetPropertyCompositeWithValueThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_XMP, "array", "value", PropertyOptions().setArray(true))
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * Empty schema and property names are rejected.
     */
    @Test
    fun testEmptySchemaAndNameThrows() {

        val xmpMeta = XMPMetaFactory.create()

        assertFailsWith<XMPException> {
            xmpMeta.setProperty("", "prop", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setProperty(XMPConst.NS_XMP, "", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getProperty("", "prop")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.getPropertyString(XMPConst.NS_XMP, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * An unregistered schema namespace is rejected.
     */
    @Test
    fun testUnregisteredSchemaThrows() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setProperty("http://example.org/not-registered/", "prop", "value")
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * The returned [XMPProperty] exposes value, options and no language.
     */
    @Test
    fun testGetPropertyDetails() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(XMPConst.NS_XMP, "prop", "value")

        val property = xmpMeta.getProperty(XMPConst.NS_XMP, "prop")!!

        assertEquals("value", property.getValue())
        assertEquals(PropertyOptions(), property.getOptions())
        assertNull(property.getLanguage())
        assertEquals("value", property.toString())
    }

    /**
     * A property path can address a value type over a composed path.
     */
    @Test
    fun testGetPropertyOverComposedPath() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setStructField(
            XMPConst.NS_XMP,
            "struct",
            XMPConst.NS_XMP,
            "count",
            "5"
        )

        assertEquals(
            expected = 5,
            actual = xmpMeta.getPropertyInteger(XMPConst.NS_XMP, "struct/xmp:count")
        )
    }

    /**
     * A test object with a recognizable string representation.
     */
    private class CustomToString {

        override fun toString(): String = "custom-value"
    }
}
