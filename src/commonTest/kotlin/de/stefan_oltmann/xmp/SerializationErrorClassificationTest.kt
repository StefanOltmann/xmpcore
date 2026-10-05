package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Serialization failures carry the documented error codes instead of a
 * generic wrap of an internal crash, so callers can dispatch on them.
 */
class SerializationErrorClassificationTest {

    /**
     * Deleting a namespace that a stored qualifier still uses is legal at
     * the registry, but the property can no longer be serialized: the
     * failure must surface as the documented BADSCHEMA error, not as a
     * generic wrap of a NullPointerException.
     */
    @Test
    fun testUnregisteredPrefixFailsAsBadSchema() {

        val xmpMeta = XMPMetaFactory.create()

        val namespace = "http://example.org/foo/"

        XMPSchemaRegistry.registerNamespace(namespace, "foo")

        xmpMeta.setProperty(XMPConst.NS_DC, "source", "value")

        xmpMeta.setQualifier(
            XMPConst.NS_DC,
            "source",
            namespace,
            "prop",
            "value"
        )

        XMPSchemaRegistry.deleteNamespace(namespace)

        val exception = assertFailsWith<XMPException> {
            XMPMetaFactory.serializeToString(xmpMeta, SerializeOptions())
        }

        assertEquals(XMPErrorConst.BADSCHEMA, exception.errorCode)
        assertTrue(exception.message!!.contains("foo"), exception.message)
    }
}
