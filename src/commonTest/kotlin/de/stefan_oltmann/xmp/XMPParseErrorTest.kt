package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the RDF parser error handling for invalid XMP documents.
 */
class XMPParseErrorTest {

    /**
     * An RDF core term as node element is rejected.
     */
    @Test
    fun testRdfCoreTermAsNodeElementThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description/>
              <rdf:resource/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A typed node at the top level is rejected.
     */
    @Test
    fun testTopLevelTypedNodeThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Bag/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADXMP) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * The rdf:parseType values Literal, Collection and Other are rejected.
     */
    @Test
    fun testUnsupportedParseTypeValuesThrow() {

        for (parseType in listOf("Literal", "Collection", "Other")) {

            val testXmp = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about=""
                      xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title rdf:parseType="$parseType">x</dc:title>
                  </rdf:Description>
                </rdf:RDF>
            """.trimIndent()

            assertXMPError(XMPErrorConst.BADXMP) {
                XMPMetaFactory.parseFromString(testXmp)
            }
        }
    }

    /**
     * An old RDF term as property element name is rejected.
     */
    @Test
    fun testOldRdfTermAsPropertyNameThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <rdf:aboutEach foo="x"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An rdf:li outside of an array is rejected.
     */
    @Test
    fun testMisplacedRdfLiThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:struct>
                  <rdf:Description>
                    <rdf:li>a</rdf:li>
                  </rdf:Description>
                </xmp:struct>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An array with an arbitrary child name is rejected.
     */
    @Test
    fun testArrayWithArbitraryChildNameThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:subject>
                  <rdf:Bag>
                    <dc:title>x</dc:title>
                  </rdf:Bag>
                </dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An rdf:value element at the top level is rejected.
     */
    @Test
    fun testTopLevelRdfValueThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="">
                <rdf:value>x</rdf:value>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * Mutually exclusive about and ID attributes are rejected.
     */
    @Test
    fun testMutuallyExclusiveAboutAndIdThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" rdf:ID="id1"/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * Mismatched top level rdf:about values are rejected.
     */
    @Test
    fun testMismatchedAboutValuesThrow() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="a"/>
              <rdf:Description rdf:about="b"/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADXMP) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An rdf:parseType attribute on a node element is rejected.
     */
    @Test
    fun testParseTypeOnNodeElementThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" rdf:parseType="Resource"/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * Nested content in an empty property element with property attributes is
     * rejected.
     */
    @Test
    fun testNestedContentWithPropertyAttrsThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:err="http://example.org/xmpcore-err/">
                <dc:title err:q="v">text</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An attribute without a namespace cannot become a struct field.
     */
    @Test
    fun testUnnamespacedAttributeThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title foo="x"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A resource property element with more than one child is rejected.
     */
    @Test
    fun testResourcePropertyWithTwoChildrenThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li>a</rdf:li>
                  </rdf:Alt>
                  <rdf:Bag/>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A combination of rdf:resource and rdf:nodeID is rejected.
     */
    @Test
    fun testResourceAndNodeIdConflictThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:resource="u" rdf:nodeID="n"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A combination of rdf:value and rdf:resource is rejected.
     */
    @Test
    fun testValueAndResourceConflictThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title rdf:value="v" rdf:resource="u"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADXMP) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A processing instruction inside the description is rejected.
     */
    @Test
    fun testProcessingInstructionInsideDescriptionThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <?adobe-xap-filters esc="CR"?>
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Titel</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An element child in a literal property element is rejected.
     */
    @Test
    fun testElementChildInLiteralPropertyThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:li>x</rdf:li>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A text child in a resource property element is rejected.
     */
    @Test
    fun testTextChildInResourcePropertyThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>text<rdf:Alt><rdf:li>a</rdf:li></rdf:Alt></dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * A typed node element with an empty namespace is tolerated with an empty
     * type name part.
     */
    @Test
    fun testTypedNodeWithEmptyNamespace() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:thing>
                  <MyType xmlns=""/>
                </xmp:thing>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertTrue(xmpMeta.doesQualifierExist(XMPConst.NS_XMP, "thing", XMPConst.NS_RDF, "type"))
    }

    /**
     * An invalid attribute on a parseType Resource property element is rejected.
     */
    @Test
    fun testInvalidParseTypeResourceAttributeThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:s rdf:parseType="Resource" foo="x"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An unrecognized RDF attribute on an empty property element is rejected.
     */
    @Test
    fun testUnrecognizedRdfAttributeThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop rdf:bagID="x"/>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * The remaining old RDF terms are rejected as property element names.
     */
    @Test
    fun testOtherOldRdfTermsAsPropertyNamesThrow() {

        for (term in listOf("aboutEachPrefix", "bagID")) {

            val testXmp = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="">
                    <rdf:$term/>
                  </rdf:Description>
                </rdf:RDF>
            """.trimIndent()

            assertXMPError(XMPErrorConst.BADRDF) {
                XMPMetaFactory.parseFromString(testXmp)
            }
        }
    }

    /**
     * A redundant xml:lang on an rdf:value element is rejected.
     */
    @Test
    fun testRedundantLangOnRdfValueThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <xmp:prop xml:lang="de">
                  <rdf:Description>
                    <rdf:value xml:lang="de">Wert</rdf:value>
                  </rdf:Description>
                </xmp:prop>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADXMP) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An rdf:datatype attribute on a node element is rejected.
     */
    @Test
    fun testDatatypeOnNodeElementThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" rdf:datatype="http://www.w3.org/2001/XMLSchema#string"/>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * An rdf:RDF element as property element is rejected.
     */
    @Test
    fun testRdfElementAsPropertyNameThrows() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Bag>
                    <rdf:RDF/>
                  </rdf:Bag>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        assertXMPError(XMPErrorConst.BADRDF) {
            XMPMetaFactory.parseFromString(testXmp)
        }
    }

    /**
     * Asserts that the given block throws an XMPException with the expected code.
     */
    private fun assertXMPError(expectedCode: Int, block: () -> Unit) {

        val ex = assertFailsWith<XMPException> {
            block()
        }

        assertEquals(expectedCode, ex.errorCode)
    }
}
