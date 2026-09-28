package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests [XMPUtils.catenateArrayItems] against the behavior of the Adobe XMPCore original,
 * including quoting and the round trip with [XMPUtils.separateArrayItems].
 */
class XMPUtilsCatenateArrayItemsTest {

    /**
     * Items are joined with the default separator "; ".
     */
    @Test
    fun testCatenatesItemsWithDefaultSeparator() {

        val xmpMeta = createItems("a", "b", "c")

        assertEquals("a; b; c", XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, false))
    }

    /**
     * A value containing a separator is quoted, and with allowCommas it stays plain.
     */
    @Test
    fun testQuotesItemsWithSeparators() {

        val xmpMeta = createItems("Smith, John")

        assertEquals(
            "\"Smith, John\"",
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, false)
        )

        assertEquals(
            "Smith, John",
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, true)
        )
    }

    /**
     * Internal quotes matching the outer pair are doubled when the value needs quoting.
     * Internal quotes alone do not need quoting, like the Adobe original documents.
     */
    @Test
    fun testDoublesInternalQuotes() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArray(true),
            "Irv \"Bud\" Jones; Jr."
        )

        /* A raw string cannot hold the quote sequences well, so the escaped form is kept. */
        @Suppress("StringShouldBeRawString")
        val expected = "\"Irv \"\"Bud\"\" Jones; Jr.\""

        assertEquals(expected, XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, false))
    }

    /**
     * Internal quotes alone do not need quoting, like the Adobe original documents.
     */
    @Test
    fun testKeepsInternalQuotesWithoutSeparators() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArray(true),
            "Irving \"Bud\" Jones"
        )

        assertEquals(
            "Irving \"Bud\" Jones",
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, false)
        )
    }

    /**
     * A missing array yields an empty string.
     */
    @Test
    fun testEmptyResultForMissingArray() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals("", XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, null, false))
    }

    /**
     * An alternate array is rejected.
     */
    @Test
    fun testRejectsAlternateArray() {

        val xmpMeta = parse(
            """
                <dc:description>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">some</rdf:li>
                  </rdf:Alt>
                </dc:description>
            """.trimIndent()
        )

        val ex = assertFailsWith<XMPException> {
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "description", null, null, false)
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * Composite array items are rejected.
     */
    @Test
    fun testRejectsCompositeItems() {

        val xmpMeta = parse(
            """
                <dc:creator>
                  <rdf:Seq>
                    <rdf:li>
                      <rdf:Description/>
                    </rdf:li>
                  </rdf:Seq>
                </dc:creator>
            """.trimIndent()
        )

        val ex = assertFailsWith<XMPException> {
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "creator", null, null, false)
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * A separator without exactly one semicolon is rejected.
     */
    @Test
    fun testRejectsInvalidSeparator() {

        val xmpMeta = createItems("a")

        val ex = assertFailsWith<XMPException> {
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", ", ", null, false)
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * A mismatched quote pair is rejected.
     */
    @Test
    fun testRejectsMismatchedQuotePair() {

        val xmpMeta = createItems("a")

        val ex = assertFailsWith<XMPException> {
            XMPUtils.catenateArrayItems(xmpMeta, XMPConst.NS_DC, "subject", null, "(]", false)
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * Catenate and separate round trip the items.
     */
    @Test
    fun testRoundTripWithSeparateArrayItems() {

        val source = createItems("Smith, John", "Doe, Jane")

        val catenated = XMPUtils.catenateArrayItems(source, XMPConst.NS_DC, "subject", null, null, false)

        val target = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            target,
            XMPConst.NS_DC,
            "subject",
            catenated,
            PropertyOptions().setArray(true),
            false
        )

        assertEquals(
            listOf("Smith, John", "Doe, Jane"),
            (1..target.countArrayItems(XMPConst.NS_DC, "subject"))
                .mapNotNull { target.getArrayItem(XMPConst.NS_DC, "subject", it)?.getValue() }
        )
    }

    private fun createItems(vararg values: String): XMPMeta {

        val xmpMeta = XMPMetaFactory.create()

        XMPUtils.separateArrayItems(
            xmpMeta,
            XMPConst.NS_DC,
            "subject",
            values.joinToString("; "),
            PropertyOptions().setArray(true),
            true
        )

        return xmpMeta
    }

    private fun parse(properties: String): XMPMeta {

        val rdf = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                $properties
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        return XMPMetaFactory.parseFromString(rdf)
    }
}
