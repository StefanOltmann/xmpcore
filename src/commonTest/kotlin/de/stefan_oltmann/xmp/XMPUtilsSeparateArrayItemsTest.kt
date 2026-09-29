package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [XMPUtils.separateArrayItems] against the behavior of the Adobe XMPCore original,
 * including quoted values, comma preservation and array form validation.
 */
class XMPUtilsSeparateArrayItemsTest {

    /**
     * A single space belongs to the value, like in the Adobe original: only multiple
     * spaces or separators split the string.
     */
    @Test
    fun testSingleSpaceStaysInsideTheValue() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "one two three",
            PropertyOptions().setArray(true),
            false
        )

        assertEquals(
            listOf("one two three"),
            readItems(xmpMeta)
        )
    }

    /**
     * Semicolons and commas separate values when commas are not preserved.
     */
    @Test
    fun testSeparatesOnSemicolesAndCommas() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "a; b, c",
            PropertyOptions().setArray(true),
            false
        )

        assertEquals(
            listOf("a", "b", "c"),
            readItems(xmpMeta)
        )
    }

    /**
     * With preserveCommas the comma stays inside the value and only semicolons separate.
     */
    @Test
    fun testPreservesCommasWithinValues() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "Smith, John; Doe, Jane",
            PropertyOptions().setArray(true),
            true
        )

        assertEquals(
            listOf("Smith, John", "Doe, Jane"),
            readItems(xmpMeta)
        )
    }

    /**
     * A quoted value keeps its inner separators, doubled quotes undouble.
     */
    @Test
    fun testSeparatesQuotedValues() {

        val xmpMeta = XMPMetaFactory.create()

        /* A raw string cannot hold the triple quote sequence well, so the escaped form
         * is kept. */
        @Suppress("StringShouldBeRawString")
        val separatedValues = "\"Irv \"\"Bud\"\" Jones\" second"

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            separatedValues,
            PropertyOptions().setArray(true),
            false
        )

        assertEquals(
            listOf("Irv \"Bud\" Jones", "second"),
            readItems(xmpMeta)
        )
    }

    /**
     * A matching old value is kept once instead of being duplicated.
     */
    @Test
    fun testKeepsMatchingOldValueOnce() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "one; two",
            PropertyOptions().setArray(true),
            false
        )

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "two; three",
            PropertyOptions().setArray(true),
            false
        )

        assertEquals(
            listOf("one", "two", "three"),
            readItems(xmpMeta)
        )
    }

    /**
     * An alternate array is rejected as target.
     */
    @Test
    fun testRejectsAlternateArray() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:description>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">some</rdf:li>
                  </rdf:Alt>
                </dc:description>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val ex = assertFailsWith<XMPException> {
            XMPUtils.separateArrayItems(
                xmpMeta,
                XMPConst.NS_DC,
                "description",
                "a b",
                PropertyOptions().setArray(true),
                false
            )
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Options with non-array bits are rejected.
     */
    @Test
    fun testRejectsNonArrayOptions() {

        val xmpMeta = XMPMetaFactory.create()

        val ex = assertFailsWith<XMPException> {
            XMPUtils.separateArrayItems(
                xmpMeta,
                XMPConst.NS_DC,
                "subject",
                "a b",
                PropertyOptions().setArray(true).setStruct(true),
                false
            )
        }

        assertEquals(XMPErrorConst.BADOPTIONS, ex.errorCode)
    }

    /**
     * Separating into an existing array of a different form is rejected.
     */
    @Test
    fun testRejectsMismatchedArrayForm() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "one",
            PropertyOptions().setArray(true),
            false
        )

        val ex = assertFailsWith<XMPException> {
            XMPUtils.separateArrayItems(
                xmpMeta,
                XMPConst.NS_DC,
                "subject",
                "two",
                PropertyOptions().setArray(true).setArrayOrdered(true),
                false
            )
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * The created array has the requested form, a bag by default.
     */
    @Test
    fun testCreatesArrayWithRequestedForm() {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            "one",
            PropertyOptions().setArray(true),
            false
        )

        val property = checkNotNull(xmpMeta.getProperty(XMPConst.NS_DC, "subject"))

        assertTrue(property.getOptions().isArray())

        assertFalse(property.getOptions().isArrayOrdered())
    }

    private fun readItems(xmpMeta: XMPMeta): List<String> =
        (1..xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
            .mapNotNull { xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", it)?.getValue() }
}
