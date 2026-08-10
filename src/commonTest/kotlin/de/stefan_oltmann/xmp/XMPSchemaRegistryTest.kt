package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
