/*
 * Copyright 2026 Stefan Oltmann
 */
package de.stefan_oltmann.xmp.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for `SerializeOptions`.
 *
 * Cloning options with the canonical format used to throw an `XMPException` because
 * `USE_CANONICAL_FORMAT` was missing from the valid options mask.
 */
class SerializeOptionsTest {

    @Test
    fun testCloneWithCanonicalFormat() {

        val options = SerializeOptions().setUseCanonicalFormat(true)

        val clone = options.clone()

        assertTrue(clone.getUseCanonicalFormat())
    }

    @Test
    fun testSetOptionsWithCanonicalFormat() {

        val options = SerializeOptions()

        options.setOptions(SerializeOptions().setUseCanonicalFormat(true).getOptions())

        assertTrue(options.getUseCanonicalFormat())
    }

    /**
     * A flag setter returns a modified copy and leaves the receiver unchanged, so a shared
     * instance can be reused from concurrent writers.
     */
    @Test
    fun testFlagSetterReturnsCopyLeavingOriginalUnchanged() {

        val original = SerializeOptions()

        val modified = original.setOmitPacketWrapper(true)

        assertTrue(modified.getOmitPacketWrapper())

        assertTrue(!original.getOmitPacketWrapper())
    }

    /**
     * A field setter returns a modified copy and leaves the receiver unchanged.
     */
    @Test
    fun testPaddingSetterReturnsCopyLeavingOriginalUnchanged() {

        val original = SerializeOptions()

        val modified = original.setPadding(100)

        assertEquals(100, modified.getPadding())

        assertEquals(0, original.getPadding())
    }

    /**
     * The fluent chaining keeps working across the copies.
     */
    @Test
    fun testChainingAcrossCopies() {

        val options = SerializeOptions()
            .setOmitPacketWrapper(true)
            .setNewline("\r\n")
            .setSort(true)

        assertTrue(options.getOmitPacketWrapper())

        assertEquals("\r\n", options.getNewline())

        assertTrue(options.getSort())
    }
}
