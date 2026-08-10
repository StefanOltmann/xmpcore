package de.stefan_oltmann.xmp.options

import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the bit handling of [Options] and the flag accessors of all option classes.
 */
class OptionsTest {

    /**
     * [Options.setOption] sets and unsets single bits.
     */
    @Test
    fun testSetOptionSetsAndUnsetsBits() {

        val options = PropertyOptions()

        options.setOption(PropertyOptions.ARRAY, true)

        assertTrue(options.isArray())

        options.setOption(PropertyOptions.ARRAY, false)

        assertFalse(options.isArray())
    }

    /**
     * Setting invalid option bits is rejected.
     */
    @Test
    fun testSetOptionsWithInvalidBitsThrows() {

        val ex = assertFailsWith<XMPException> {
            PropertyOptions().setOptions(0x00000001)
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * The constructor with an invalid bitmask is rejected.
     */
    @Test
    fun testConstructorWithInvalidBitsThrows() {

        val ex = assertFailsWith<XMPException> {
            PropertyOptions(0x00000001)
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * Options are equal when their bitmasks are equal.
     */
    @Test
    fun testEqualsAndHashCode() {

        val first = PropertyOptions().setArray(true)
        val second = PropertyOptions().setArray(true)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertFalse(first == PropertyOptions())
    }

    /**
     * The string representation is the hex bitmask.
     */
    @Test
    fun testToStringIsHexBitmask() {

        assertEquals("0x200", PropertyOptions().setArray(true).toString())
    }

    /**
     * Every property option flag has a getter and setter round trip.
     */
    @Test
    fun testPropertyOptionsFlagRoundTrips() {

        val options = PropertyOptions()
            .setURI(true)
            .setHasQualifiers(true)
            .setQualifier(true)
            .setHasLanguage(true)
            .setHasType(true)
            .setStruct(true)
            .setArray(true)
            .setArrayOrdered(true)
            .setArrayAlternate(true)
            .setArrayAltText(true)
            .setSchemaNode(true)

        assertTrue(options.isURI())
        assertTrue(options.hasQualifiers())
        assertTrue(options.isQualifier())
        assertTrue(options.hasLanguage())
        assertTrue(options.hasType())
        assertTrue(options.isStruct())
        assertTrue(options.isArray())
        assertTrue(options.isArrayOrdered())
        assertTrue(options.isArrayAlternate())
        assertTrue(options.isArrayAltText())
        assertTrue(options.isSchemaNode())
    }

    /**
     * [PropertyOptions.isCompositeProperty] and [PropertyOptions.isSimple]
     * report the composite state.
     */
    @Test
    fun testCompositeAndSimpleChecks() {

        val struct = PropertyOptions().setStruct(true)

        assertTrue(struct.isCompositeProperty())
        assertFalse(struct.isSimple())

        val array = PropertyOptions().setArray(true)

        assertTrue(array.isCompositeProperty())
        assertFalse(array.isSimple())

        val simple = PropertyOptions()

        assertFalse(simple.isCompositeProperty())
        assertTrue(simple.isSimple())
    }

    /**
     * [PropertyOptions.isOnlyArrayOptions] only accepts array form bits.
     */
    @Test
    fun testIsOnlyArrayOptions() {

        assertTrue(PropertyOptions().setArray(true).isOnlyArrayOptions())
        assertFalse(PropertyOptions().setURI(true).isOnlyArrayOptions())
        assertFalse(PropertyOptions().setStruct(true).isOnlyArrayOptions())
    }

    /**
     * [PropertyOptions.equalArrayTypes] compares the array form bits only.
     */
    @Test
    fun testEqualArrayTypes() {

        val bag = PropertyOptions().setArray(true)
        val ordered = PropertyOptions().setArray(true).setArrayOrdered(true)

        assertTrue(bag.equalArrayTypes(PropertyOptions().setArray(true)))
        assertFalse(bag.equalArrayTypes(ordered))
    }

    /**
     * [PropertyOptions.mergeWith] combines both bitmasks.
     */
    @Test
    fun testMergeWith() {

        val options = PropertyOptions().setArray(true)

        options.mergeWith(PropertyOptions().setArrayOrdered(true))

        assertTrue(options.isArray())
        assertTrue(options.isArrayOrdered())
    }

    /**
     * Struct and array options are mutually exclusive.
     */
    @Test
    fun testStructAndArrayConflictThrows() {

        val ex = assertFailsWith<XMPException> {
            PropertyOptions().setOptions(PropertyOptions.STRUCT or PropertyOptions.ARRAY)
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * URI and composite options are mutually exclusive.
     */
    @Test
    fun testUriAndCompositeConflictThrows() {

        assertFailsWith<XMPException> {
            PropertyOptions().setOptions(PropertyOptions.URI or PropertyOptions.ARRAY)
        }.let { assertEquals(XMPErrorConst.BADOPTIONS, it.errorCode) }

        assertFailsWith<XMPException> {
            PropertyOptions().setOptions(PropertyOptions.URI or PropertyOptions.STRUCT)
        }.let { assertEquals(XMPErrorConst.BADOPTIONS, it.errorCode) }
    }

    /**
     * A default [AliasOptions] object is a direct alias.
     */
    @Test
    fun testAliasOptionsIsSimpleByDefault() {

        val options = AliasOptions()

        assertTrue(options.isSimple())
        assertFalse(options.isArray())
    }

    /**
     * The array form setters promote all lower array bits.
     */
    @Test
    fun testAliasOptionsArrayPromotion() {

        val ordered = AliasOptions().setArrayOrdered(true)

        assertTrue(ordered.isArray())
        assertTrue(ordered.isArrayOrdered())
        assertFalse(ordered.isArrayAlternate())

        val altText = AliasOptions().setArrayAltText(true)

        assertTrue(altText.isArray())
        assertTrue(altText.isArrayOrdered())
        assertTrue(altText.isArrayAlternate())
        assertTrue(altText.isArrayAltText())
        assertFalse(altText.isSimple())
    }

    /**
     * [AliasOptions.toPropertyOptions] keeps the bits.
     */
    @Test
    fun testAliasOptionsToPropertyOptions() {

        val propertyOptions = AliasOptions().setArrayAlternate(true).toPropertyOptions()

        assertTrue(propertyOptions.isArrayAlternate())
        assertTrue(propertyOptions.isArrayOrdered())
        assertTrue(propertyOptions.isArray())
    }

    /**
     * The parse options flags have getter and setter round trips.
     */
    @Test
    fun testParseOptionsFlagRoundTrips() {

        val options = ParseOptions()
            .setRequireXMPMeta(true)
            .setStrictAliasing(true)
            .setOmitNormalization(true)

        assertTrue(options.getRequireXMPMeta())
        assertTrue(options.getStrictAliasing())
        assertTrue(options.getOmitNormalization())
    }

    /**
     * The iterator options flags have getter and setter round trips.
     */
    @Test
    fun testIteratorOptionsFlagRoundTrips() {

        val options = IteratorOptions()
            .setJustChildren(true)
            .setJustLeafname(true)
            .setJustLeafnodes(true)
            .setOmitQualifiers(true)

        assertTrue(options.isJustChildren())
        assertTrue(options.isJustLeafname())
        assertTrue(options.isJustLeafnodes())
        assertTrue(options.isOmitQualifiers())
    }

    /**
     * The serialize options flags have getter and setter round trips.
     */
    @Test
    fun testSerializeOptionsFlagRoundTrips() {

        val options = SerializeOptions()
            .setOmitPacketWrapper(true)
            .setOmitXmpMetaElement(true)
            .setReadOnlyPacket(true)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(true)
            .setSort(true)

        assertTrue(options.getOmitPacketWrapper())
        assertTrue(options.getOmitXmpMetaElement())
        assertTrue(options.getReadOnlyPacket())
        assertTrue(options.getUseCompactFormat())
        assertTrue(options.getUseCanonicalFormat())
        assertTrue(options.getSort())
    }

    /**
     * Invalid bits are rejected for all option classes.
     */
    @Test
    fun testInvalidBitsRejectedForAllOptionClasses() {

        /* Bit 0x2 is not a valid option of any of the option classes. */
        val invalid = 0x00000002

        assertFailsWith<XMPException> {
            ParseOptions().setOptions(invalid)
        }
        assertFailsWith<XMPException> {
            IteratorOptions().setOptions(invalid)
        }
        assertFailsWith<XMPException> {
            SerializeOptions().setOptions(invalid)
        }
        assertFailsWith<XMPException> {
            AliasOptions().setOptions(invalid)
        }
    }

    /**
     * All property option bits have defined names.
     */
    @Test
    fun testAllPropertyOptionNamesDefined() {

        val options = PropertyOptions()
            .setURI(true)
            .setHasQualifiers(true)
            .setQualifier(true)
            .setHasLanguage(true)
            .setHasType(true)
            .setStruct(true)
            .setArray(true)
            .setArrayOrdered(true)
            .setArrayAlternate(true)
            .setArrayAltText(true)
            .setSchemaNode(true)

        assertEquals(
            expected = "URI | HAS_QUALIFIER | QUALIFIER | HAS_LANGUAGE | HAS_TYPE | STRUCT | " +
                "ARRAY | ARRAY_ORDERED | ARRAY_ALTERNATE | ARRAY_ALT_TEXT | SCHEMA_NODE",
            actual = options.getOptionsString()
        )
    }

    /**
     * All iterator option bits have defined names.
     */
    @Test
    fun testAllIteratorOptionNamesDefined() {

        val options = IteratorOptions()
            .setJustChildren(true)
            .setJustLeafnodes(true)
            .setJustLeafname(true)
            .setOmitQualifiers(true)

        assertEquals(
            expected = "JUST_CHILDREN | JUST_LEAFNODES | JUST_LEAFNAME | OMIT_QUALIFIERS",
            actual = options.getOptionsString()
        )
    }

    /**
     * All parse option bits have defined names.
     */
    @Test
    fun testAllParseOptionNamesDefined() {

        val options = ParseOptions()
            .setRequireXMPMeta(true)
            .setStrictAliasing(true)
            .setOmitNormalization(true)

        assertEquals(
            expected = "REQUIRE_XMP_META | STRICT_ALIASING | OMIT_NORMALIZATION",
            actual = options.getOptionsString()
        )
    }

    /**
     * All serialize option bits have defined names, except the canonical format.
     */
    @Test
    fun testAllSerializeOptionNamesDefined() {

        val options = SerializeOptions()
            .setOmitPacketWrapper(true)
            .setReadOnlyPacket(true)
            .setUseCompactFormat(true)
            .setUseCanonicalFormat(true)
            .setOmitXmpMetaElement(true)
            .setSort(true)

        assertEquals(
            expected = "OMIT_PACKET_WRAPPER | READONLY_PACKET | USE_COMPACT_FORMAT | " +
                "<option name not defined> | OMIT_XMPMETA_ELEMENT | NORMALIZED",
            actual = options.getOptionsString()
        )
    }

    /**
     * All alias option bits have defined names.
     */
    @Test
    fun testAllAliasOptionNamesDefined() {

        val options = AliasOptions().setArrayAltText(true)

        assertEquals(
            expected = "ARRAY | ARRAY_ORDERED | ARRAY_ALTERNATE | ARRAY_ALT_TEXT",
            actual = options.getOptionsString()
        )
    }

    /**
     * [AliasOptions.setArray] unsets the array bit.
     */
    @Test
    fun testAliasOptionsSetArrayFalse() {

        val options = AliasOptions().setArray(true)

        options.setArray(false)

        assertFalse(options.isArray())
    }

    /**
     * The DELETE_EXISTING bit has no defined name.
     */
    @Test
    fun testDeleteExistingOptionHasNoName() {

        val options = PropertyOptions()

        options.setOption(PropertyOptions.DELETE_EXISTING, true)

        assertEquals("<option name not defined>", options.getOptionsString())
    }
}
