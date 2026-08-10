/*
 * Copyright 2026 Stefan Oltmann
 */
package de.stefan_oltmann.xmp.options

import kotlin.test.Test
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
}
