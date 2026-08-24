package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.AliasOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the namespace and alias functions of [XMPSchemaRegistry].
 *
 * Every test uses its own namespace URI because the registry is a global
 * singleton that keeps all registrations for the whole test run.
 */
class XMPSchemaRegistryFeaturesTest {

    /**
     * Re-registering an existing namespace returns the original prefix.
     */
    @Test
    fun testRegisterNamespaceReturnsExistingPrefix() {

        val namespace = "http://example.org/xmpcore-registry-existing/"

        XMPSchemaRegistry.registerNamespace(namespace, "regExisting")

        assertEquals(
            expected = "regExisting:",
            actual = XMPSchemaRegistry.registerNamespace(namespace, "other")
        )
    }

    /**
     * A namespace whose suggested prefix is taken gets a generated prefix.
     */
    @Test
    fun testRegisterNamespaceGeneratesUniquePrefix() {

        val firstNamespace = "http://example.org/xmpcore-registry-prefix-first/"
        val secondNamespace = "http://example.org/xmpcore-registry-prefix-second/"

        XMPSchemaRegistry.registerNamespace(firstNamespace, "regPrefix")

        val generated = XMPSchemaRegistry.registerNamespace(secondNamespace, "regPrefix")

        assertEquals("regPrefix_1_:", generated)
    }

    /**
     * Empty URI and prefix are rejected, bad XML names are rejected.
     */
    @Test
    fun testRegisterNamespaceInvalidParamsThrows() {

        val namespace = "http://example.org/xmpcore-registry-invalid/"

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerNamespace("", "p")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerNamespace(namespace, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerNamespace(namespace, "1bad")
        }.let { assertEquals(XMPErrorConst.BADXML, it.errorCode) }
    }

    /**
     * The prefix and URI lookups are inverse operations, with and without colon.
     */
    @Test
    fun testGetNamespacePrefixAndUri() {

        val namespace = "http://example.org/xmpcore-registry-lookup/"

        XMPSchemaRegistry.registerNamespace(namespace, "regLookup")

        assertEquals("regLookup:", XMPSchemaRegistry.getNamespacePrefix(namespace))
        assertEquals(namespace, XMPSchemaRegistry.getNamespaceURI("regLookup:"))
        assertEquals(namespace, XMPSchemaRegistry.getNamespaceURI("regLookup"))

        assertNull(XMPSchemaRegistry.getNamespacePrefix("http://example.org/unknown/"))
        assertNull(XMPSchemaRegistry.getNamespaceURI("unknown"))
    }

    /**
     * The registered namespaces are visible in both maps.
     */
    @Test
    fun testGetNamespacesAndPrefixes() {

        val namespace = "http://example.org/xmpcore-registry-maps/"

        XMPSchemaRegistry.registerNamespace(namespace, "regMaps")

        assertEquals("regMaps:", XMPSchemaRegistry.getNamespaces()[namespace])
        assertEquals(namespace, XMPSchemaRegistry.getPrefixes()["regMaps:"])
    }

    /**
     * Deleting a namespace removes it from both maps.
     */
    @Test
    fun testDeleteNamespace() {

        val namespace = "http://example.org/xmpcore-registry-delete/"

        XMPSchemaRegistry.registerNamespace(namespace, "regDelete")

        XMPSchemaRegistry.deleteNamespace(namespace)

        assertNull(XMPSchemaRegistry.getNamespacePrefix(namespace))
        assertNull(XMPSchemaRegistry.getNamespaceURI("regDelete:"))

        /* Deleting an unknown namespace does nothing. */
        XMPSchemaRegistry.deleteNamespace("http://example.org/unknown/")
    }

    /**
     * A registered alias can be resolved in all lookup functions.
     */
    @Test
    fun testResolveAndFindAlias() {

        val namespace = "http://example.org/xmpcore-registry-alias/"

        XMPSchemaRegistry.registerNamespace(namespace, "regAlias")

        XMPSchemaRegistry.registerAlias(
            namespace,
            "AliasProp",
            XMPConst.NS_DC,
            "title",
            null
        )

        val resolved = XMPSchemaRegistry.resolveAlias(namespace, "AliasProp")

        assertNotNull(resolved)
        assertEquals(XMPConst.NS_DC, resolved.getNamespace())
        assertEquals("dc:", resolved.getPrefix())
        assertEquals("title", resolved.getPropName())
        assertTrue(resolved.getAliasForm().isSimple())

        assertEquals(
            expected = XMPConst.NS_DC,
            actual = XMPSchemaRegistry.findAlias("regAlias:AliasProp")?.getNamespace()
        )
        assertEquals(
            expected = XMPConst.NS_DC,
            actual = XMPSchemaRegistry.findAliases(namespace).single().getNamespace()
        )
    }

    /**
     * Unknown aliases resolve to null.
     */
    @Test
    fun testResolveUnknownAliasReturnsNull() {

        val namespace = "http://example.org/xmpcore-registry-unknown/"

        XMPSchemaRegistry.registerNamespace(namespace, "regUnknown")

        assertNull(XMPSchemaRegistry.resolveAlias(namespace, "UnknownProp"))
        assertNull(XMPSchemaRegistry.findAlias("regUnknown:UnknownProp"))
        assertEquals(emptySet(), XMPSchemaRegistry.findAliases(namespace))
    }

    /**
     * Registering an alias with an array form reports that form.
     */
    @Test
    fun testRegisterAliasWithArrayForm() {

        val namespace = "http://example.org/xmpcore-registry-form/"

        XMPSchemaRegistry.registerNamespace(namespace, "regForm")

        XMPSchemaRegistry.registerAlias(
            namespace,
            "AltProp",
            XMPConst.NS_DC,
            "title",
            AliasOptions().setArrayAltText(true)
        )

        val alias = checkNotNull(XMPSchemaRegistry.resolveAlias(namespace, "AltProp"))

        assertTrue(alias.getAliasForm().isArrayAltText())
        assertTrue(alias.getAliasForm().isArray())
    }

    /**
     * Registering the same alias twice is rejected.
     */
    @Test
    fun testRegisterAliasDuplicateThrows() {

        val namespace = "http://example.org/xmpcore-registry-duplicate/"

        XMPSchemaRegistry.registerNamespace(namespace, "regDup")

        XMPSchemaRegistry.registerAlias(
            namespace,
            "DupProp",
            XMPConst.NS_DC,
            "title",
            null
        )

        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                namespace,
                "DupProp",
                XMPConst.NS_DC,
                "description",
                null
            )
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * Aliasing to a property that is already an alias is rejected.
     */
    @Test
    fun testRegisterAliasActualAlreadyAliasThrows() {

        val namespace = "http://example.org/xmpcore-registry-chain/"

        XMPSchemaRegistry.registerNamespace(namespace, "regChain")

        /* xmp:Title is a standard alias, so it cannot be an actual property. */
        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                namespace,
                "ChainSecond",
                XMPConst.NS_XMP,
                "Title",
                null
            )
        }

        assertEquals(XMPErrorConst.BADPARAM, ex.errorCode)
    }

    /**
     * Registering an alias with unregistered namespaces is rejected.
     */
    @Test
    fun testRegisterAliasUnregisteredNamespaceThrows() {

        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                "http://example.org/unknown-alias/",
                "Prop",
                XMPConst.NS_DC,
                "title",
                null
            )
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * Registering an alias with empty parameters is rejected.
     */
    @Test
    fun testRegisterAliasEmptyParamsThrows() {

        val namespace = "http://example.org/xmpcore-registry-empty/"

        XMPSchemaRegistry.registerNamespace(namespace, "regEmpty")

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias("", "Prop", XMPConst.NS_DC, "title", null)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(namespace, "", XMPConst.NS_DC, "title", null)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(namespace, "Prop", "", "title", null)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(namespace, "Prop", XMPConst.NS_DC, "", null)
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * The standard aliases are registered, e.g. xmp:Title to dc:title.
     */
    @Test
    fun testStandardAliasesAreRegistered() {

        val alias = XMPSchemaRegistry.resolveAlias(XMPConst.NS_XMP, "Title")

        assertNotNull(alias)
        assertEquals(XMPConst.NS_DC, alias.getNamespace())
        assertEquals("title", alias.getPropName())

        assertFalse(XMPSchemaRegistry.getAliases().isEmpty())
    }

    /**
     * Finding aliases of an unregistered namespace yields an empty set.
     */
    @Test
    fun testFindAliasesForUnregisteredNamespace() {

        assertEquals(
            expected = emptySet(),
            actual = XMPSchemaRegistry.findAliases("http://example.org/not-registered/")
        )
    }

    /**
     * Registering an alias with an unregistered actual namespace is rejected.
     */
    @Test
    fun testRegisterAliasUnregisteredActualNamespaceThrows() {

        val namespace = "http://example.org/xmpcore-registry-actual/"

        XMPSchemaRegistry.registerNamespace(namespace, "regActual")

        val ex = assertFailsWith<XMPException> {
            XMPSchemaRegistry.registerAlias(
                namespace,
                "Prop",
                "http://example.org/not-registered/",
                "title",
                null
            )
        }

        assertEquals(XMPErrorConst.BADSCHEMA, ex.errorCode)
    }

    /**
     * The alias info describes its base property in the string form.
     */
    @Test
    fun testAliasInfoToString() {

        val alias = checkNotNull(XMPSchemaRegistry.resolveAlias(XMPConst.NS_XMP, "Title"))

        val stringForm = alias.toString()

        assertTrue(stringForm.contains("dc:title"))
        assertTrue(stringForm.contains(XMPConst.NS_DC))
    }

    /**
     * A prefix that already ends with a colon is kept as is.
     */
    @Test
    fun testRegisterNamespaceAcceptsPrefixWithColon() {

        val namespace = "http://example.org/xmpcore-registry-colon/"

        XMPSchemaRegistry.registerNamespace(namespace, "regColon:")

        assertEquals("regColon:", XMPSchemaRegistry.getNamespacePrefix(namespace))
    }
}
