package de.stefan_oltmann.xmp.internal

import de.stefan_oltmann.xmp.XMPConst
import de.stefan_oltmann.xmp.XMPException
import de.stefan_oltmann.xmp.XMPSchemaRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the XMP path expression parser [XMPPathParser].
 */
class XMPPathParserTest {

    /**
     * A simple property expands to the schema step and the prefixed root property.
     */
    @Test
    fun testExpandSimplePath() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "subject")

        assertEquals(2, path.size())
        assertEquals(XMPConst.NS_DC, path.getSegment(0).name)
        assertEquals("dc:subject", path.getSegment(1).name)
        assertEquals(XMPPath.STRUCT_FIELD_STEP, path.getSegment(1).kind)
    }

    /**
     * A numeric array index becomes an array index step.
     */
    @Test
    fun testExpandArrayIndexPath() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "subject[2]")

        assertEquals(3, path.size())
        assertEquals("[2]", path.getSegment(2).name)
        assertEquals(XMPPath.ARRAY_INDEX_STEP, path.getSegment(2).kind)
        assertEquals("dc:subject[2]", path.toString())
    }

    /**
     * All documented shorthand forms of an array index are equivalent.
     */
    @Test
    fun testExpandArrayIndexShorthands() {

        val forms = listOf("subject[2]", "subject/[2]", "subject*[2]", "subject/*[2]")

        for (form in forms) {

            val path = XMPPathParser.expandXPath(XMPConst.NS_DC, form)

            assertEquals("dc:subject[2]", path.toString())
        }
    }

    /**
     * The last-item selector becomes an array last step.
     */
    @Test
    fun testExpandArrayLastStep() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "subject[last()]")

        assertEquals(XMPPath.ARRAY_LAST_STEP, path.getSegment(2).kind)
        assertEquals("dc:subject[last()]", path.toString())
    }

    /**
     * A struct field path adds a struct field step.
     */
    @Test
    fun testExpandStructFieldPath() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_XMP, "Regions/mwg-rs:RegionList")

        assertEquals(3, path.size())
        assertEquals("mwg-rs:RegionList", path.getSegment(2).name)
        assertEquals(XMPPath.STRUCT_FIELD_STEP, path.getSegment(2).kind)
        assertEquals("xmp:Regions/mwg-rs:RegionList", path.toString())
    }

    /**
     * A qualifier path step gets the leading question mark.
     */
    @Test
    fun testExpandQualifierPath() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_XMP, "Rating/?xmp:qual")

        assertEquals("?xmp:qual", path.getSegment(2).name)
        assertEquals(XMPPath.QUALIFIER_STEP, path.getSegment(2).kind)
        assertEquals("xmp:Rating/?xmp:qual", path.toString())
    }

    /**
     * The at-sign shorthand for xml:lang is converted to a qualifier step.
     */
    @Test
    fun testExpandAtSignXmlLangShorthand() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "title/@xml:lang")

        assertEquals("?xml:lang", path.getSegment(2).name)
        assertEquals(XMPPath.QUALIFIER_STEP, path.getSegment(2).kind)
    }

    /**
     * The at-sign is only allowed for xml:lang.
     */
    @Test
    fun testExpandAtSignOtherThanXmlLangThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "title/@dc:foo")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A field selector becomes a field selector step.
     */
    @Test
    fun testExpandFieldSelector() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "subject[dc:name='fox']")

        assertEquals(XMPPath.FIELD_SELECTOR_STEP, path.getSegment(2).kind)
        assertEquals("[dc:name='fox']", path.getSegment(2).name)
    }

    /**
     * A qualifier selector becomes a qualifier selector step.
     */
    @Test
    fun testExpandQualifierSelector() {

        /*
         * The prefix must be registered here, because the test must not depend on the
         * registration side effects of other test classes in the shared registry.
         */
        XMPSchemaRegistry.registerNamespace("http://example.org/xmpcore-test/", "test")

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "subject[?test:qual='value']")

        assertEquals(XMPPath.QUAL_SELECTOR_STEP, path.getSegment(2).kind)
        assertEquals("[?test:qual='value']", path.getSegment(2).name)
    }

    /**
     * A qualified property name at the root is accepted when it matches the schema.
     */
    @Test
    fun testExpandQualifiedRootProperty() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_XMP, "xmp:Rating")

        assertEquals("xmp:Rating", path.getSegment(1).name)
    }

    /**
     * A root prefix that does not match the schema namespace is rejected.
     */
    @Test
    fun testExpandMismatchedRootPrefixThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "xmp:Rating")
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * An unregistered schema namespace is rejected.
     */
    @Test
    fun testExpandUnregisteredSchemaThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath("http://example.org/not-registered/", "prop")
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * A top level qualifier name is rejected.
     */
    @Test
    fun testExpandTopLevelQualifierThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "?title")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A path with a slash in the root property is rejected.
     */
    @Test
    fun testExpandComplexRootPropertyThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a/b")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An empty path segment is rejected.
     */
    @Test
    fun testExpandEmptySegmentThrows() {

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a/")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a//b")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * An asterisk without a following bracket is rejected.
     */
    @Test
    fun testExpandAsteriskWithoutBracketThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a*")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A non-numeric array index other than last() is rejected.
     */
    @Test
    fun testExpandInvalidArrayIndexThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[abc]")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An unquoted selector value is rejected.
     */
    @Test
    fun testExpandUnquotedSelectorValueThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[dc:b=value]")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An unterminated selector value is rejected.
     */
    @Test
    fun testExpandUnterminatedSelectorValueThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[dc:b='value]")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An alias root is resolved to its base property.
     */
    @Test
    fun testExpandSimpleAlias() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_XMP, "Title")

        assertEquals(XMPConst.NS_DC, path.getSegment(0).name)
        assertEquals("dc:title", path.getSegment(1).name)
        assertTrue(path.getSegment(1).isAlias)
        assertEquals(0, path.getSegment(1).aliasForm)
    }

    /**
     * An alias to an alt-text array adds the x-default qualifier selector step.
     */
    @Test
    fun testExpandAltTextAlias() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_PDF, "Title")

        assertEquals(3, path.size())
        assertEquals(XMPPath.QUAL_SELECTOR_STEP, path.getSegment(2).kind)
        assertEquals("[?xml:lang='x-default']", path.getSegment(2).name)
        assertTrue(path.getSegment(2).isAlias)
        assertFalse(path.getSegment(2).aliasForm == 0)
    }

    /**
     * An alias to an ordered array adds the first-item index step.
     */
    @Test
    fun testExpandArrayAlias() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_XMP, "Author")

        assertEquals(3, path.size())
        assertEquals(XMPPath.ARRAY_INDEX_STEP, path.getSegment(2).kind)
        assertEquals("[1]", path.getSegment(2).name)
    }

    /**
     * Null parameters are rejected.
     */
    @Test
    fun testExpandNullParamsThrows() {

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(null, "prop")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, null)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * An empty path is rejected.
     */
    @Test
    fun testExpandEmptyPathThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An invalid root XML name is rejected.
     */
    @Test
    fun testExpandInvalidRootNameThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "1bad")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * An empty schema namespace is rejected.
     */
    @Test
    fun testExpandEmptySchemaThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath("", "prop")
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * A bracket at the very end of the path is rejected instead of reading
     * past the end of the string.
     */
    @Test
    fun testExpandBracketAtEndOfPathThrows() {

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[dc:b=")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * Selector forms without a closing bracket are rejected.
     */
    @Test
    fun testExpandUnclosedSelectorThrows() {

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[b")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[2")
        }.let { assertEquals(XMPErrorConst.BADXPATH, it.errorCode) }
    }

    /**
     * A qualifier with an unknown prefix is rejected.
     */
    @Test
    fun testExpandUnknownQualifierPrefixThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a/?unknown:q")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * The at-sign in a field selector is only allowed for xml:lang.
     */
    @Test
    fun testExpandAtSignInFieldSelectorThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPPathParser.expandXPath(XMPConst.NS_DC, "a[@dc:name='x']")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * A doubled quote inside a selector value is kept.
     */
    @Test
    fun testExpandSelectorWithDoubledQuote() {

        val path = XMPPathParser.expandXPath(XMPConst.NS_DC, "a[dc:name='it''s']")

        assertEquals(XMPPath.FIELD_SELECTOR_STEP, path.getSegment(2).kind)
        assertEquals("[dc:name='it''s']", path.getSegment(2).name)
    }
}
