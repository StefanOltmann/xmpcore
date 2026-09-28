package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the alias registration validation of the [XMPSchemaRegistry].
 */
class XMPSchemaRegistryTest {

    /**
     * Registering an alias whose name contains a path character is rejected.
     */
    @Test
    fun testRegisterAliasRejectsPathCharactersInAliasName() {

        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                "http://example.org/alias-test/",
                "simpleAlias/sub",
                "http://example.org/actual-test/",
                "actualProp",
                null
            )
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Registering an alias whose actual name contains a path character is rejected.
     */
    @Test
    fun testRegisterAliasRejectsPathCharactersInActualName() {

        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                "http://example.org/alias-test/",
                "simpleAlias",
                "http://example.org/actual-test/",
                "actualProp[1]",
                null
            )
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Registering an alias with simple names between registered namespaces succeeds.
     */
    @Test
    fun testRegisterAliasAcceptsSimpleNames() {

        XMPSchemaRegistry.registerNamespace("http://example.org/alias-test/", "aliasTest")
        XMPSchemaRegistry.registerNamespace("http://example.org/actual-test/", "actualTest")

        XMPSchemaRegistry.registerAlias(
            "http://example.org/alias-test/",
            "simpleAlias",
            "http://example.org/actual-test/",
            "actualProp",
            null
        )
    }

    /**
     * A document that declares a standard namespace under its usual prefix keeps that
     * prefix in the output instead of receiving a generated one.
     */
    @Test
    fun testParseKeepsStandardMicrosoftPhotoPrefix() {

        /* language=XML */
        val testXmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:MicrosoftPhoto="http://ns.microsoft.com/photo/1.0/"
                  MicrosoftPhoto:Rating="4"/>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(4, xmpMeta.getPropertyInteger(XMPConst.NS_MICROSOFT_PHOTO, "Rating"))

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("MicrosoftPhoto:"))

        assertTrue(!serialized.contains("MicrosoftPhoto_1_:"))
    }

    /**
     * A new namespace whose suggested prefix is already taken gets a generated prefix
     * with the deterministic "_1_:" scheme.
     */
    @Test
    fun testRegisterNamespaceGeneratesDeterministicPrefixOnCollision() {

        XMPSchemaRegistry.registerNamespace("https://example.org/reg-test/base/", "zzRegA")

        val prefix = XMPSchemaRegistry.registerNamespace("https://example.org/reg-test/collision/", "zzRegA")

        assertEquals("zzRegA_1_:", prefix)
    }

    /**
     * Two unknown namespaces whose declared prefixes collide with registered or generated
     * prefixes receive their generated prefixes in namespace URI order, so the assignment
     * does not depend on the platform's DOM attribute order.
     */
    @Test
    fun testParseAssignsCollidingGeneratedPrefixesDeterministically() {

        XMPSchemaRegistry.registerNamespace("https://example.org/reg-test/base2/", "zzColl")

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:zzColl="https://example.org/reg-test/fake-dc/"
                  xmlns:zzColl_1_="https://example.org/reg-test/fake-dc-one/"
                  zzColl:note="a"
                  zzColl_1_:note="b"/>
            </rdf:RDF>
        """.trimIndent()

        XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            "zzColl_1_:",
            XMPSchemaRegistry.getNamespacePrefix("https://example.org/reg-test/fake-dc-one/")
        )

        assertEquals(
            "zzColl_2_:",
            XMPSchemaRegistry.getNamespacePrefix("https://example.org/reg-test/fake-dc/")
        )
    }
}
