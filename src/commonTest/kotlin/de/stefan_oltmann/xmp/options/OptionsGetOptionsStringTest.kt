/*
 * Copyright 2026 Stefan Oltmann
 */
package de.stefan_oltmann.xmp.options

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the human-readable option names of [Options.getOptionsString].
 */
class OptionsGetOptionsStringTest {

    /**
     * An options object without set bits is reported as `<none>`.
     */
    @Test
    fun testNoOptionsYieldsNone() {

        assertEquals(
            expected = "<none>",
            actual = PropertyOptions().getOptionsString()
        )
    }

    /**
     * The names of the set property options are joined in the order of their bits.
     */
    @Test
    fun testPropertyOptionsNames() {

        val options = PropertyOptions()
            .setStruct(true)
            .setArrayOrdered(true)
            .setArrayAltText(true)

        assertEquals(
            expected = "STRUCT | ARRAY_ORDERED | ARRAY_ALT_TEXT",
            actual = options.getOptionsString()
        )
    }

    /**
     * The names of the set serialize options are joined, the sort bit uses its
     * legacy name NORMALIZED.
     */
    @Test
    fun testSerializeOptionsNames() {

        val options = SerializeOptions()
            .setReadOnlyPacket(true)
            .setSort(true)

        assertEquals(
            expected = "READONLY_PACKET | NORMALIZED",
            actual = options.getOptionsString()
        )
    }

    /**
     * An option bit without a defined name is reported with a placeholder.
     */
    @Test
    fun testUndefinedOptionNameFallback() {

        val options = SerializeOptions().setUseCanonicalFormat(true)

        assertEquals(
            expected = "<option name not defined>",
            actual = options.getOptionsString()
        )
    }

    /**
     * The names of the set parse options are joined.
     */
    @Test
    fun testParseOptionsNames() {

        val options = ParseOptions()
            .setStrictAliasing(true)
            .setOmitNormalization(true)

        assertEquals(
            expected = "STRICT_ALIASING | OMIT_NORMALIZATION",
            actual = options.getOptionsString()
        )
    }

    /**
     * The names of the set iterator options are joined.
     */
    @Test
    fun testIteratorOptionsNames() {

        val options = IteratorOptions()
            .setJustLeafname(true)
            .setOmitQualifiers(true)

        assertEquals(
            expected = "JUST_LEAFNAME | OMIT_QUALIFIERS",
            actual = options.getOptionsString()
        )
    }

    /**
     * The setter of the alternate array form reports all promoted array bits.
     */
    @Test
    fun testAliasOptionsNames() {

        val options = AliasOptions().setArrayAlternate(true)

        assertEquals(
            expected = "ARRAY | ARRAY_ORDERED | ARRAY_ALTERNATE",
            actual = options.getOptionsString()
        )
    }
}
