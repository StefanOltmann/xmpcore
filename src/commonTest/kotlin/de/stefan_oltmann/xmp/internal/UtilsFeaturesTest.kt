package de.stefan_oltmann.xmp.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the remaining utility functions of [Utils]: language normalization,
 * selector splitting, UUID checks, XML name checks and XML escaping.
 */
class UtilsFeaturesTest {

    /**
     * The primary language subtag is lowercased, the 2-letter secondary subtag
     * is uppercased and all other subtags are lowercased.
     */
    @Test
    fun testNormalizeLangValue() {

        assertEquals("en", Utils.normalizeLangValue("EN"))
        assertEquals("de-DE", Utils.normalizeLangValue("DE-de"))
        assertEquals("en-US", Utils.normalizeLangValue("en-US"))
        assertEquals("x-default", Utils.normalizeLangValue("x-default"))
        assertEquals("zh-HANS-cn", Utils.normalizeLangValue("ZH-HANS-CN"))
    }

    /**
     * Underscores in language tags are converted to hyphens, spaces are dropped.
     */
    @Test
    fun testNormalizeLangValueSeparators() {

        assertEquals("de-DE", Utils.normalizeLangValue("de_de"))
        assertEquals("enus", Utils.normalizeLangValue("EN US"))
    }

    /**
     * Splits a qualifier selector into name and value.
     */
    @Test
    fun testSplitNameAndValue() {

        val result = Utils.splitNameAndValue("[?test:qual='value']")

        assertEquals("test:qual", result[0])
        assertEquals("value", result[1])
    }

    /**
     * Splits a field selector, a doubled quote inside the value is reduced to one.
     */
    @Test
    fun testSplitNameAndValueWithDoubledQuote() {

        val result = Utils.splitNameAndValue("[dc:field=\"it's\"]")

        assertEquals("dc:field", result[0])
        assertEquals("it's", result[1])
    }

    /**
     * Splits a selector with an empty value.
     */
    @Test
    fun testSplitNameAndValueWithEmptyValue() {

        val result = Utils.splitNameAndValue("[dc:field='']")

        assertEquals("dc:field", result[0])
        assertEquals("", result[1])
    }

    /**
     * A well-formed UUID is accepted.
     */
    @Test
    fun testCheckUUIDFormatAcceptsWellFormedUuid() {

        assertTrue(Utils.checkUUIDFormat("bac965c4-9d87-11d9-9a30-000d936b79c4"))
    }

    /**
     * Null and wrongly placed or missing dashes are rejected.
     */
    @Test
    fun testCheckUUIDFormatRejectsMalformedUuid() {

        assertFalse(Utils.checkUUIDFormat(null))
        assertFalse(Utils.checkUUIDFormat("bac965c49d87-11d9-9a30-000d936b79c4"))
        assertFalse(Utils.checkUUIDFormat("bac965c4-9d87-11d9-9a30-000d936b79c"))
        assertFalse(Utils.checkUUIDFormat("bac965c4-9d87-11d9-9a30-000d936b79c4x"))
    }

    /**
     * Accepts names starting with letters, underscores and colons.
     */
    @Test
    fun testIsXMLNameAcceptsValidNames() {

        assertTrue(Utils.isXMLName("simple"))
        assertTrue(Utils.isXMLName("_private"))
        assertTrue(Utils.isXMLName("name1"))
        assertTrue(Utils.isXMLName("name-with-dash"))
        assertTrue(Utils.isXMLName("name.with-dot"))
        assertTrue(Utils.isXMLName("a:b"))
    }

    /**
     * Rejects names that do not start with an XML name start char.
     */
    @Test
    fun testIsXMLNameRejectsInvalidNames() {

        assertFalse(Utils.isXMLName("1abc"))
        assertFalse(Utils.isXMLName("-abc"))
        assertFalse(Utils.isXMLName(".abc"))
    }

    /**
     * Rejects names with an invalid char after the first one.
     */
    @Test
    fun testIsXMLNameRejectsInvalidInnerChar() {

        assertFalse(Utils.isXMLName("a b"))
        assertFalse(Utils.isXMLName("a: b"))
    }

    /**
     * Rejects names containing colons, unlike [Utils.isXMLName].
     */
    @Test
    fun testIsXMLNameNSRejectsColons() {

        assertTrue(Utils.isXMLNameNS("simple"))
        assertFalse(Utils.isXMLNameNS("a:b"))
        assertFalse(Utils.isXMLNameNS(":a"))
    }

    /**
     * Accepts names with non-ASCII XML name chars.
     */
    @Test
    fun testIsXMLNameAcceptsNonAsciiChars() {

        /* Combining mark as non-start char. */
        assertTrue(Utils.isXMLName("a\u0300"))
        /* Start char from the 0x100-0x2FF range. */
        assertTrue(Utils.isXMLName("\u0100abc"))
        /* Start char from the 0x2C00-0x2FEF range. */
        assertTrue(Utils.isXMLName("\u2E00abc"))
    }

    /**
     * Rejects invalid non-ASCII start chars.
     */
    @Test
    fun testIsXMLNameRejectsInvalidNonAsciiStartChars() {

        /* Combining mark is no valid start char. */
        assertFalse(Utils.isXMLName("\u0300abc"))
    }

    /**
     * Escapes the XML special characters in element text.
     */
    @Test
    fun testEscapeXMLText() {

        assertEquals(
            "a&lt;b&gt;c&amp;d",
            Utils.escapeXML("a<b>c&d", false, false)
        )
    }

    /**
     * Escapes quotes additionally in attribute values.
     */
    @Test
    fun testEscapeXMLAttribute() {

        assertEquals(
            "a&quot;b",
            Utils.escapeXML("a\"b", true, false)
        )
        /* Quotes are not escaped in element text. */
        assertEquals("a\"b", Utils.escapeXML("a\"b", false, false))
    }

    /**
     * Escapes whitespace control chars as numeric character references.
     */
    @Test
    fun testEscapeXMLWhitespaces() {

        assertEquals("a&#x9;b", Utils.escapeXML("a\tb", false, true))
        assertEquals("a&#xA;b", Utils.escapeXML("a\nb", false, true))
        assertEquals("a&#xD;b", Utils.escapeXML("a\rb", false, true))
    }

    /**
     * Whitespace control chars stay untouched when not asked to be escaped.
     */
    @Test
    fun testEscapeXMLKeepsWhitespacesIfNotRequested() {

        assertEquals("a\tb", Utils.escapeXML("a\tb", false, false))
    }

    /**
     * A string without special characters is returned unchanged.
     */
    @Test
    fun testEscapeXMLWithoutSpecialChars() {

        assertEquals("plain text", Utils.escapeXML("plain text", true, true))
    }
}
